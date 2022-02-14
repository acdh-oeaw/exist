export vers=5.3.0
docker pull gcr.io/distroless/java:11
mkdir patches
cp -a $vers patches/$vers
git reset --hard
git checkout eXist-$vers
for patch in `ls patches/$vers`
do patch -p1 < patches/$vers/$patch
done
mvn -V -B -q -Pdocker -DskipTests -Ddependency-check.skip=true -Ddocker.tag=$vers-java11-ShenGC -Ddocker.org=acdhch clean package
docker run -dit --rm -p 8080:8080 --name exist-ci --rm acdhch/existdb:$vers-java11-ShenGC
docker login
mvn -q -Ddocker.tag=$vers-java11-ShenGC -Ddocker.org=acdhch -Ddocker.username=$DOCKER_USERNAME -Ddocker.password=$DOCKER_PASSWORD docker:build docker:push