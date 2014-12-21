HNAP1-web-interface-enabler
===========================

Enables web access to your Linksys router via HNAP api. Tested against Cisco Linksys E3200 firmware 1.0.05.

Usage:
run 
java -jar enableRouterWebAccess.jar 192.168.1.1 admin YourSecretPassword
where 192.168.1.1 is your router address, (may start with https://)
admin - default username
YourSecretPassword - router password

Latest E3200 firmware doesn't let you to login without using Cisco Connect which doesn't work when WiFi is off, 
so using the HNAP may be the only option to let you access admin web page for your router.
