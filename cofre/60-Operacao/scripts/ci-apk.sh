#!/bin/bash
# Espera o último build terminar e mostra o resultado + erros.
B=https://api.github.com/repos/huryelprofissional-design/novel-app
SHA=$(git -C ${REPO:-.} rev-parse HEAD)
for i in $(seq 1 90); do
  R=$(curl -s "$B/actions/runs?head_sha=$SHA&per_page=5")
  ST=$(echo "$R" | python3 -c "import json,sys;r=[x for x in json.load(sys.stdin)['workflow_runs'] if x['name']=='Gerar APK'];print(r[0]['status']+' '+str(r[0]['conclusion'])+' '+str(r[0]['id']) if r else 'none')")
  set -- $ST
  if [ "$1" = "completed" ]; then
    echo "RESULTADO: $2 (run $3)"
    for J in $(curl -s "$B/actions/runs/$3/jobs" | python3 -c "import json,sys;[print(j['id']) for j in json.load(sys.stdin)['jobs']]"); do
      curl -s "$B/check-runs/$J/annotations?per_page=60" | python3 -c "
import json,sys
for a in json.load(sys.stdin):
  m=a.get('message','')
  if 'deprecated' in m or 'ubuntu-latest' in m or 'Node.js' in m: continue
  print(m)"
    done
    exit 0
  fi
  sleep 20
done
echo "tempo esgotado"
