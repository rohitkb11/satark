"""Reproducible, dependency-free training. Synthetic development data, not real-world proof."""
import csv, hashlib, json, math, pathlib, random, re, unicodedata

ROOT = pathlib.Path(__file__).resolve().parents[1]
DIM = 4096
SEED = 42
# Entire templates/campaigns belong to one split. No template crosses splits.
TEMPLATES = {
    'train': [
        (1, 'Bank verification desk: provide your password to keep account access.'),
        (1, 'Your account is locked. Send your OTP to our support agent.'),
        (1, 'Pay a release fee to receive your pending refund of {amount}.'),
        (1, 'You won a reward. Transfer {amount} to claim the prize.'),
        (1, 'Install a remote access app so our bank officer can fix your account.'),
        (1, 'Your parcel is held. Submit card PIN and security code to release it.'),
        (1, 'KYC expires today. Enter your banking credentials on our verification form.'),
        (1, 'We are the payment support team. Disclose your verification code over this chat.'),
        (1, 'Deposit {amount} first and we will unlock your cashback reward.'),
        (1, 'Account closure warning. Reveal the login password for immediate assistance.'),
        (0, 'Your order has shipped. Track the parcel in the official shopping app.'),
        (0, 'Your refund of {amount} has been credited. No action is needed.'),
        (0, 'Your OTP is {code}. Do not share it with anyone, including bank staff.'),
        (0, 'Never pay a fee to receive a refund. Beware of messages requesting money.'),
        (0, 'Visit a bank branch to update KYC. We never ask for passwords or OTPs.'),
        (0, 'Payment of {amount} received. View the receipt in your account.'),
        (0, 'Do not install remote access apps at the request of unknown callers.'),
        (0, 'Your package will arrive tomorrow between two and four.'),
        (0, 'Please bring the documents to our meeting. See you on Monday.'),
        (0, 'We will never ask you to disclose your PIN, password, or verification code.'),
    ],
    'validation': [
        (1, 'Customer security team: tell us your one time password to restore services.'),
        (1, 'Pay {amount} upfront to get your cashback benefit.'),
        (1, 'Send your card password to our delivery agent for parcel approval.'),
        (1, 'Reward department requires a deposit before sending your prize.'),
        (0, 'Security reminder: do not tell anyone your one time password.'),
        (0, 'Your cashback benefit was added. You do not need to pay any fee.'),
        (0, 'Parcel delivered successfully. Thanks for shopping with us.'),
        (0, 'Your bank statement is available in the official app.'),
    ],
    'test': [
        (1, 'For account recovery our officer needs your login secret in this conversation.'),
        (1, 'Provide a verification password to the support representative.'),
        (1, 'To obtain the refund, send {amount} to the agent first.'),
        (1, 'Claim your lottery winnings by depositing the processing charge.'),
        (1, 'Give the security code from your card to our verification team.'),
        (0, 'Bank safety tip: keep your login secret private and never send it in a chat.'),
        (0, 'The refund is complete. No deposit or processing charge is required.'),
        (0, 'A support representative will never request your verification password.'),
        (0, 'Your purchase receipt is ready. Open the shopping app to view it.'),
        (0, 'Lunch at one? Bring your notebook and we can finish the assignment.'),
    ]
}

def jhash(s):
    value = 0
    for ch in s: value = (31 * value + ord(ch)) & 0xffffffff
    return (value if value < 0x80000000 else value - 0x100000000) % DIM

def features(text):
    text = unicodedata.normalize('NFKC', text).lower()
    text = re.sub(r'https?://\S+|www\.\S+', ' ', text)
    words = re.findall(r'[a-z]+', text)[:256]
    indices = set()
    for i, word in enumerate(words):
        indices.add(jhash('w:' + word))
        if i: indices.add(jhash('b:' + words[i-1] + ':' + word))
        token = '^' + word + '$'
        for size in (3, 4, 5):
            for start in range(len(token)-size+1): indices.add(jhash('c:' + token[start:start+size]))
    return sorted(indices)

def generate():
    rows = []
    for split, templates in TEMPLATES.items():
        for i, (label, template) in enumerate(templates):
            variants = []
            for amount, code in [('250', '824917'), ('1500', '391782'), ('4200', '762194')]:
                text = template.format(amount=amount, code=code)
                for prefix in ['', 'Notice: ', 'Hello customer. ']: variants.append(prefix + text)
            for v, text in enumerate(dict.fromkeys(variants)):
                rows.append({'id': f'{split}-{i}-{v}', 'campaign': f'{split}-{i}', 'split': split,
                             'label': label, 'text': text})
    return rows

def sigmoid(z): return 1 / (1 + math.exp(-max(-35, min(35, z))))
def metrics(rows, weights, bias, threshold):
    tp = fp = tn = fn = 0
    for row in rows:
        pred = sigmoid(bias + sum(weights[j] for j in features(row['text']))) >= threshold
        if pred and row['label']: tp += 1
        elif pred: fp += 1
        elif row['label']: fn += 1
        else: tn += 1
    return {'count':len(rows), 'tp':tp,'fp':fp,'tn':tn,'fn':fn,
            'precision':tp/max(1,tp+fp),'recall':tp/max(1,tp+fn),
            'false_warning_rate':fp/max(1,fp+tn)}

def main():
    rows = generate()
    dataset = ROOT/'model/data/synthetic_messages.tsv'
    dataset.parent.mkdir(parents=True, exist_ok=True)
    with dataset.open('w', newline='', encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=['id','campaign','split','label','text'],delimiter='\t')
        writer.writeheader();writer.writerows(rows)
    train=[(features(r['text']),r['label']) for r in rows if r['split']=='train']
    weights=[0.0]*DIM;bias=0.0;rng=random.Random(SEED)
    for epoch in range(160):
        rng.shuffle(train); lr=0.045/(1+epoch/40)
        for indices,y in train:
            err=sigmoid(bias+sum(weights[j] for j in indices))-y
            bias-=lr*err
            for j in indices:weights[j]-=lr*(err+0.005*weights[j])
    validation=[r for r in rows if r['split']=='validation']
    # Threshold selected using validation only, conservative false-warning constraint.
    choices=[]
    for threshold in [x/100 for x in range(50,96)]:
        m=metrics(validation,weights,bias,threshold)
        if m['false_warning_rate'] <= 0.10:choices.append((m['recall'],threshold))
    threshold=max(choices)[1] if choices else 0.95
    report={
        'model':'synthetic-linear-v1','algorithm':'hashed word/bigram/character features + logistic regression',
        'dimensions':DIM,'seed':SEED,'threshold':threshold,
        'dataset_sha256':hashlib.sha256(dataset.read_bytes()).hexdigest(),
        'split_policy':'Whole campaign templates are exclusive to train, validation, or test.',
        'warning':'Synthetic, AI-assisted development data. Not independently validated and not real-world accuracy.',
        'train':metrics([r for r in rows if r['split']=='train'],weights,bias,threshold),
        'validation':metrics(validation,weights,bias,threshold),
        'test':metrics([r for r in rows if r['split']=='test'],weights,bias,threshold),
        'probes':[]}
    for r in rows:
        if r['split']=='test' and r['id'].endswith('-0'):
            report['probes'].append({'text':r['text'],'score':sigmoid(bias+sum(weights[j] for j in features(r['text'])))})
    dest=ROOT/'core/src/main/resources/message_model.tsv'
    with dest.open('w',encoding='utf-8',newline='\n') as f:
        f.write(f'scamcheck-linear-v1\n{DIM}\n{bias:.12g}\n{threshold:.12g}\n')
        for weight in weights:f.write(f'{weight:.12g}\n')
    (ROOT/'model/evaluation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ['threshold','train','validation','test','warning']},indent=2))

if __name__=='__main__':main()
