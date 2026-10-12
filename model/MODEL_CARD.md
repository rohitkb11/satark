# Experimental local text model

## Intended use

An opt-in development feature that suggests manual review of English message wording. It never independently issues a Strong warning, never overrides rule evidence, and never declares a message safe. It is disabled by default.

## Implementation

- ID: synthetic-linear-v1.
- Logistic regression on binary hashed word, word-pair and character n-gram features, 4096 dimensions.
- Training: dependency-free Python script, deterministic seed 42.
- Inference: pure Java, no third-party AI runtime and no internet requests.
- Bundled file: `core/src/main/resources/message_model.tsv`.
- File stores version, dimension, intercept, review threshold, and weights. Invalid/missing data produces an explicit rules-only fallback.
- The internal sigmoid score is an uncalibrated model score, not the probability of fraud. It is not shown as a confidence percentage in the app.

## Data and provenance

162 generated development messages from 38 authored campaign templates. Entire templates belong to one split: 96 training, 30 validation, 36 test messages. Variants change introductions, amounts and example codes, so rows are correlated within a template; they are not independent observations. There is no customer data, external dataset or third-party model weight licence. Templates and training code were created with AI assistance for this project. No claim of independent annotation or evaluation is made.

The threshold (0.63) was selected using validation examples only. The test results were retained as measured, rather than used to retune the model after observing false positives.

## Held-out synthetic development results

Model alone: TP=21, FP=6, TN=9, FN=0. Six of fifteen benign messages were incorrectly flagged for review. Precision=77.8%, recall=100%, benign review rate=40% **on this tiny synthetic set only**. These are not production or real-world accuracy numbers, and template correlation means they should not be read as a statistical performance estimate.

Integrated comparison, with positive defined as any review outcome (Needs checking or Strong warning):

| Mode | TP | FP | TN | FN |
| --- | ---: | ---: | ---: | ---: |
| Rules only | 3 | 0 | 15 | 18 |
| Rules + experimental model | 21 | 6 | 9 | 0 |

Decision: ship the experimental switch off by default. The model adds wording coverage but fails a reasonable false-warning target. A representative, consented/publicly authorised evaluation set and stronger training are needed before default activation. Never hide the measured false warnings in the competition submission.

## Reproduce

```powershell
python scripts/train_model.py
.\scripts\build.ps1 -Test
java -cp build/classes in.scamcheck.cli.Evaluate model/data/synthetic_messages.tsv
```

Python uses only standard-library modules. The JSON report includes dataset SHA-256, split metrics and cross-language inference probes. A Java test checks a score against the Python reference to catch feature/hash mismatches.

## Limitations

Small synthetic data, simple wording features, no conversation context, incomplete negation handling, no supported Hindi/Hinglish accuracy, no live site/sender checks, no adversarial robustness claim. Non-Latin scripts skip the English model. A short English classifier cannot distinguish all scams from legitimate messages with the same words.
