# Bundled sources and disclosure

## Public Suffix List

- Source: https://publicsuffix.org/list/public_suffix_list.dat
- Provider documentation: https://publicsuffix.org/list/
- Retrieved for development: 2026-10-12.
- Upstream snapshot version: 2026-10-07_07-28-19_UTC.
- Upstream commit: 3929462652695bad04f0a27afb600974014a3c8b.
- SHA-256: BA7D836C0EA57A8BCF9F1FCB868066A7554BED029B441ECC6204AD8494A32612.
- Licence: Mozilla Public License 2.0; retained in the data header. Full text: https://mozilla.org/MPL/2.0/.
- The parser includes private suffixes, wildcard rules and exception rules. It uses the snapshot offline; it never refreshes while checking a message.

## Small organisation-address registry

Checked 2026-10-12 using the primary sites below. Entries identify saved website roots only. This is not an exhaustive partner-domain list, sender authentication or live ownership verification.

| Organisation | Saved roots | Primary source |
| --- | --- | --- |
| HDFC Bank | hdfc.bank.in, hdfcbank.com | https://www.hdfc.bank.in/; https://www.hdfcbank.com/ redirects there |
| ICICI Bank | icici.bank.in, icicibank.com | https://www.icici.bank.in/; https://www.icicibank.com/ redirects there |
| SBI | sbi.bank.in | https://sbi.bank.in/ |
| Amazon India | amazon.in | https://www.amazon.in/ |
| Flipkart | flipkart.com | https://www.flipkart.com/ |

No source page text, brand logos or customer records are bundled. The registry stores website facts and source URLs. An absence from this registry means unknown, not fraudulent. Additional real domains require checking before being added.

## Rules and test examples

The rules and synthetic scenarios were authored for this MVP with AI assistance. No customer messages, confidential bank data, training datasets or third-party model weights are included. The test cases are disclosed as engineering fixtures, not real-world evaluation evidence.

No code has been copied from the previously researched detection repositories. Phase 2's local model is trained from this project's synthetic authored examples, with no third-party weights. See `model/MODEL_CARD.md`. A future external reputation feed still needs a separate licence/provenance review.
