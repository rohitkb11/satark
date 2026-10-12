"""Package already-verified public MVP outputs; never tools, private keys or user data."""
from pathlib import Path
import hashlib,json,zipfile,subprocess
root=Path(__file__).resolve().parents[1]
files=['output/Satark-debug.apk','README.md','docs/PHASE_4.md','docs/DEMO_GUIDE.md','docs/PHASE_3.md','docs/DATA_SOURCES.md','model/MODEL_CARD.md','evaluation/phase4-results.json','evaluation/phase4-device.json','evaluation/phase4-offline.txt','output/ScamCheck-update-v1.scu','output/ScamCheck-update-v2.scu']
for name in files:
    if not (root/name).is_file():raise SystemExit('Missing required artifact: '+name)
manifest={'version':'0.4.0','date':'2026-10-12','source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'build':'debug, locally signed; build steps reproducible but debug key/timing prevent byte-identical results across machines','submission':'PDF deferred at owner request; real-device acceptance remains unexecuted','sha256':{name:hashlib.sha256((root/name).read_bytes()).hexdigest() for name in files}}
(root/'output/artifact-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
with zipfile.ZipFile(root/'output/Satark-MVP-v0.4.0.zip','w',zipfile.ZIP_DEFLATED) as archive:
    for name in files:archive.write(root/name,name)
    archive.write(root/'output/artifact-manifest.json','artifact-manifest.json')
print('MVP package and checksums written. Private tools/keys excluded.')
