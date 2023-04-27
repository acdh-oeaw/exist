:: run 'set vers=6.0.1' or similar
docker pull gcr.io/distroless/java11-debian11
docker pull openjdk:11-slim-bullsey
xcopy /E %vers% patches\%vers%\
git reset --hard
git checkout master
git branch --delete --force %vers%-java11-ShenGC
mkdir patches\%vers%
del /s /q patches\%vers%\*
del /s /q *.yml exist-core\src\*.* exist-deb\pom.xml exist-jetty-config\src\main\resources\org\exist\jetty\etc\*.* extensions\betterform\*.* extensions\contentextraction\src\test\*.* extensions\exquery\*.*
git reset --hard
xcopy /E /Y %vers% patches\%vers%\
git checkout eXist-%vers% -b %vers%-java11-ShenGC
for /f %%p in ('dir /b patches\%vers%') do "C:\Program Files\Git\usr\bin\patch.exe" -p1 < patches\%vers%\%%p
mvn -DskipTests -Pdocker clean package
goto end
:: The next lines need to be executed manually
docker tag existdb/existdb:%vers% acdhch/existdb:%vers%-java11-ShenGC
docker rmi existdb/existdb:%vers%
docker run --rm -it -p 8080:8080 -v %cd%\logs:/exist/logs acdhch/existdb:%vers%-java11-ShenGC
grype\grype db update
grype\grype docker:acdhch/existdb:%vers%-java11-ShenGC --only-fixed | grep Critical
docker login
docker push acdhch/existdb:%vers%-java11-ShenGC
:end