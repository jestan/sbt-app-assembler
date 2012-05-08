java -Duser.name=mysql -Xmx1024M -Xss2M -XX:PermSize=96m -XX:MaxPermSize=500m -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -jar `dirname $0`/sbt-launch.jar "$@"

