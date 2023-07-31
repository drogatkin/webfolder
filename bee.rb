# a rustbee script to build webfolder 

project =webfolder
"build_directory" = ./bin
source_directory ="src/java7"
doc_directory=doc
build_file ="${project}.jar"
war_file ="${project}.war"
 domain ="msn"
resources ="${domain}.${project}.resources"
manifestf =""
main_class= "${domain}.${project}.Main"

websocket jar=${build_directory}/.temp_repo/javax.websocket-api-1.1.jar
servlet jar=${build_directory}/.temp_repo/javax.servlet-api-3.1.0.jar

aldan3_home="/home/dmitriy/projects/aldan3"
aldan3=${aldan3_home}${~separator~}build${~separator~}aldan3.jar
aldan3-jdo=${aldan3_home}-jdo${~separator~}build${~separator~}aldan3-jdo.jar
webbee="/home/dmitriy/projects/Webbee/build/out/webbee.jar"

cp=${servlet jar}${~path_separator~}${websocket jar}${~path_separator~}${aldan3}${~path_separator~}${aldan3-jdo}${~path_separator~}${webbee}${~path_separator~}${build_directory}

target clean {
    dependency {true}
    exec rm  (
        -r,
        ${build_directory}/${domain},
        ${build_directory}/${build_file}
    )
}

target dep_dir {
  dependency {
     eq {
        timestamp(${build_directory}${~separator~}.temp_repo)
     }
   }
   display(Dir ${build_directory}${~separator~}.temp_repo -p)
   exec mkdir (
        -p,
        ${build_directory}${~separator~}.temp_repo
   )
}

target get_deps {
  dependency {
    or{
     eq {
        timestamp(websocket jar)
     }
    eq {
        timestamp(servlet jar)
     }
   }
   }
   {
      if {
       eq {
          timestamp(websocket jar)
       }
       then {
         display(Loading WS)
         websocket_api="javax.websocket:javax.websocket-api:1.1":rep-maven
         as_url(websocket_api)
         exec wget (
           ~~, 
           -O,
           websocket jar
         )
      }
   }

    if {
       eq {
          timestamp(servlet jar)
       }
       then {
         servlet_api="javax.servlet:javax.servlet-api:3.1.0":rep-maven
         as_url(servlet_api)
         exec wget (
           ~~, 
           -O,
           servlet jar
         )
      }
   }
  }
}

target compile:. {
   dependency {
     dependency {
          target(get_deps)
      }
       or {
              newerthan(${source_directory}/.java,${build_directory}/.class)
       }
   }
   {
        display(Compiling Java 7+ src ...)
       newerthan(${source_directory}/.java,${build_directory}/.class)
       assign(main src,~~)
       exec javac (
         -d,
         ${build_directory},
        -classpath,
         ${cp},
         main src
       )     
      if {
         neq(${~~}, 0)
         then {
            panic("Compilation error(s)")
         }
     }
   }
}

target jar {
      dependency {
         anynewer(${build_directory}/${domain}/*,${build_directory}/${build_file})
      }
      dependency {
          target(compile)
      }
     
     {    display(Jarring ${build_file} ...)
          exec jar (
            -cf,
            ${build_directory}/${build_file},
            -C,
            ${build_directory},
            ${domain}
          )
     }
}

target test {
     dependency {
          target(jar)
     }
     dependency {
         true }
     {    display(Testing ${build_file} ...)
          exec java (
            -cp,
            ${cp},
            msn.javaarchitect.webfolder.ctrl.Folder
         )
     }
}