ssh -i website.pem ec2-user@ec2-52-11-105-90.us-west-2.compute.amazonaws.com


sudo yum install tomcat7

sudo yum install tomcat7-webapps tomcat7-docs-webapp tomcat7-admin-webapps
#change useraccess in tomcat_users.xml
sudo service tomcat7 start
#go to website public DNS:8080
#manager upload war file and deloy
##access from there or enter URL
