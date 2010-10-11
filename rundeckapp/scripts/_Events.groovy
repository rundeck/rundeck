includeTargets << grailsScript("BuildBundle")

eventCreateWarEnd = {  file,dir->
   println "Finished WAR file creation for: ${file}"
   bundleAll()
}
