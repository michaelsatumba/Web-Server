# csc667-web-server

## Info
- **THERE IS NO MASTER / MAIN BRANCH. WHEN STARTING THE REPO, BRANCH CHAU_DEV WAS THE FIRST BRANCH PUSHED TO THE REPO AFTER SWITCHING FROM OUR TEMPORARY PERSONAL PRIVATE REPO AND THERE WAS NO WAY FOR US TO CHANGE THIS. PLEASE USE CHAU_DEV IN PLACE OF MASTER. WE APOLOGIZE FOR THIS INCONVENIENCE.**
- There are commits after the deadline because we forgot to change the paths for httpd.conf and because of error during merging. Please accept this most recent commit.

## Milestones / Specifications

- [ ] 1. Read, and store, standard configuration files for use in responding to client requests - AC
- [ ] 2. Parse HTTP Requests - AC
- [ ] 3. Generate and send HTTP Responses (this involves many possible code paths, and is probably the most significant implementation step) - MS
- [ ] 4. Respond to multiple simultaneous requests through the use of threads - AC
- [ ] 5. Execute server side processes to handle server side scripts - AC
- [ ] 6. Support simple authentication - AC
- [ ] 7. Support simple caching - MS
- [ ] 8. Logging - MS

## Server Workflow

![image](https://user-images.githubusercontent.com/68071075/134900964-552f296d-bdfb-4d1e-98c3-c1afae770a6f.png)

## Run

```
// TODO: Simplify to javac WebServer.java;java WebServer
javac server/config/MimeTypes.java; javac server/config/HttpdConfig.java;javac server/config/Htpassword.java; javac public_html/RunScript.java;javac server/config/Configuration.java;javac server/config/HtAccess.java;javac server/Server.java;javac server/Worker.java;javac WebServer.java;java WebServer
```
