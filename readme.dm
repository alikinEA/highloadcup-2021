build:
docker build -t highloadcup-2021 . --force-rm=true

docker tag highloadcup-2021 stor.highloadcup.ru/rally/pike_worker
docker push stor.highloadcup.ru/rally/pike_worker
