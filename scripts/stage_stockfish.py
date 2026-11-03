#!/usr/bin/env python3
"""Stage the exact Stockfish sf_19 Android release binaries from original tarballs.
Requires Python 3.9+. Does not download/install anything. Fail closed.
"""
import argparse
import hashlib
import json
import pathlib
import tarfile

EXPECTED = {
 'arm64-v8a': {'archive':'stockfish-android-arm64-universal.tar.gz', 'elf_sha256':'ffd8fc2004d3d19f9fdab95d84c92709aea92e8d3af405eda0b281b23cf1daf8'},
 'armeabi-v7a': {'archive':'stockfish-android-armv7-neon.tar.gz', 'elf_sha256':'993a3e8bce85a503abc900056019691057a83e13ae92c1a2cbb089b9680593f3'},
}
def hashbytes(b):return hashlib.sha256(b).hexdigest()
def stage(archives, root, metadata):
 root=pathlib.Path(root).resolve()
 provenance=json.loads(pathlib.Path(metadata).read_text(encoding='utf-8'))
 for abi,data in EXPECTED.items():
  path=pathlib.Path(archives)/data['archive']
  if not path.is_file():raise RuntimeError('HOLD_MISSING_STOCKFISH_ARCHIVE_'+abi)
  expected=provenance.get('official_binaries',{}).get(data['archive'],'')
  if not expected.startswith('sha256:') or hashbytes(path.read_bytes())!=expected[7:]:
   raise RuntimeError('HOLD_ORIGINAL_ARCHIVE_SHA_'+abi)
  with tarfile.open(path,'r:gz') as tf:
   selected=[m for m in tf.getmembers() if m.isfile() and pathlib.PurePosixPath(m.name).name.startswith('stockfish-android-') and 1000000<m.size<210000000]
   if len(selected)!=1:raise RuntimeError('HOLD_ARCHIVE_MEMBER_'+abi)
   data_bytes=tf.extractfile(selected[0]).read()
  if hashbytes(data_bytes)!=data['elf_sha256']:raise RuntimeError('HOLD_ELF_HASH_'+abi)
  out=root/'android'/'app'/'src'/'main'/'jniLibs'/abi/'libstockfish.so'
  out.parent.mkdir(parents=True,exist_ok=True)
  if out.exists():raise RuntimeError('HOLD_NO_OVERWRITE_'+str(out))
  out.write_bytes(data_bytes)
  print('STAGED='+abi+';ELF_SHA256='+data['elf_sha256'])
if __name__=='__main__':
 p=argparse.ArgumentParser()
 p.add_argument('--archives',required=True)
 p.add_argument('--project',default='.')
 p.add_argument('--metadata',default='docs/STOCKFISH_PROVENANCE.json')
 a=p.parse_args()
 stage(a.archives,a.project,a.metadata)
