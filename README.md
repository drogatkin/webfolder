# WebFolder

## Purpose
Explore and operate on a remote file system using a web interface.

## Overview
As a software professional I always need to manage more than one computer and it is a 
very common running different OSes and platforms on different hardware. 
I am not a big fan of command line tools however I am not big fan of GUI either.

>My favorite tool is the FAR file manager which generally works well even across network, 
but you still need to manage to install some ftp server to make access more unified. 
So finally I came to an idea utilize web browser. Mostly every imaginable platform can run it, 
so only one part was under question is server side. 
 
Certainly I do not have luxury to install a full blown app server to run a certain 
server side program as PHP or Python module, and even Node.js. Using Java servlets brings you even 
more trouble as installing some sophisticated tools as Tomcat/Jboss and Spring. So I 
decided to come with something a really light weight, which you can carry on a single 
diskette. (BTW a serverless approach can perfectly work too)

I think I could achieve my goal and WebFolder is simple one file program as big 
as 1MB and runs on all operation systems but iOS. I do not see big deal with 
iOS, because the OS design prevents managing a sharable file space anyway.

Note, if running the file manager on Android platform isn't a requirement and PHP is acceptable, then I strongly recommend to look in
[tiny file manager](https://github.com/prasathmani/tinyfilemanager/tree/master), it's a very worthy web file manager with the same features set
offered by the WebFolder.
    
## Features

1. Browse content of a remote machine file system     
2. Sorting files view by name, size and date                                                                   
3. Create a directory                              
4. Upload and download files                        
5. Multiple files download including directories           
6. Manipulate with ZIP content (Java 7 required)
7. Multiple files upload for HTML5 capable browsers  
8. Rename files and directories                       
9. Move and copy files within a remote system       
10. Obtain download lists                              
11. Access environment variables of remote system      
12. Edit/create files of a remote system               
13. Delete files and directories                                       
14. Inspect HTTP headers sent by a browser              
15. A password protected access                          
16. Multi windows work                                 
17. No limitation on uploaded or downloaded file size  
18. Editing file attributes, time and owner           
19. Execution commands on a target system
20. Change a line ending, very useful for Linux scripts

## Design

WebBee framework was chosen. It operates web blocks which are page services underneath. So I implemented 4 major page services as
* File list view based on a tabular view block
* Download and zip service based on a stream block
* File editing service based on a form block
* Delete service based on a message block 

I have implemented both desktop and mobile service views for all services to comfortably work with the program from any device type.
 I had to tune a little behavior to fit in specific and bugs of mobile and desktop browsers. I made sure that my implementation has
  no problems to run on Dalvik VM. The implementation doesn’t use any session data so different views and operations can be performed
   in different windows of a browser asynchronously without interfering. 

## Java 7 and up
Java 7 version of WebFolder utilizes NIO file package and Java 7 languages extension. 
Unfortunately nio.file implementation is a bit sluggish. On benefit side it allows operation with zip file content.
The program keeps compatibility with Java 8, but can run on any Java from 7 to 21.


## Configuring ACE programmer editor
WebFolder is coming with preconfigured [ACE editor](https://ace.c9.io) for Java, JavaScript, batch and other common format files. However if you desire
to modify the set as adding new formats, or modifying existing ones then you can use the guide. Get the [sources](https://github.com/ajaxorg/ace-builds/) of the editor first,
and extract a desirable version in [src/3rd_party/ace](https://github.com/drogatkin/webfolder/tree/master/src/3rd_party/ace) directory.

1. Support editable file extensions are configured in webfolder.properties as property **ace_edit_exts**
        predefined value looks like: **ace_edit_exts**=*.java.js.xml.h.html.diff.c.cpp.bat.sh.json.jsp.diff.properties*
2. Modify the property above providing a desirable file extensions list. Specifying the property as empty or removing it will disable ACE extension</li>
3. Edit [edit_modes.json](https://github.com/drogatkin/webfolder/blob/master/src/html/edit_modes.json) specify JSON mapping entries for file extension and ACE mode
4. Copy addtional ACE modes files as needed to ace/ directory when build the project

## Version
The current version is 1.8. A work on 1.9 has almost finished.

## Screens

![a directory view](https://github.com/drogatkin/webfolder/blob/master/doc/screen%20shot1.png?raw=true)
![a console view](https://github.com/drogatkin/webfolder/blob/master/doc/screen%20shot2.png?raw=true)
