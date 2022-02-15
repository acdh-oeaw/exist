:: run 'set vers=5.2.0' or similar
docker pull gcr.io/distroless/java:11
mkdir patches
xcopy /E %vers% patches\
git reset --hard
git checkout eXist-%vers%
for /f %%p in ('dir /b patches\%vers%') do "C:\Program Files\Git\usr\bin\patch.exe" -p1 < patches\%vers%\%%p
mvn -DskipTests -Pdocker clean package
:: The next lines need to be executed manually
docker tag existdb/existdb:%vers% acdhch/existdb:%vers%-java11-ShenGC
docker rmi existdb/existdb:%vers%
docker run --rm -it -p 8080:8080 -v %cd%\logs:/exist/logs acdhch/existdb:%vers%-java11-ShenGC
grype\grype db update
grype\grype docker:acdhch/existdb:%vers%-java11-ShenGC --only-fixed | grep Critical
docker login
docker push acdhch/existdb:%vers%-java11-ShenGC