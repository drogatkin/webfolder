 <!-- //
    function selectAll() {
       if (document.forms.folder.elements.files.length == undefined)
          document.forms.folder.elements.files.checked = document.forms.folder.elements.files.checked == false;
       else  
         for(var el=0,n= document.forms.folder.elements.files.length; el<n; el++) {
           if (//window.getComputedStyle(document.forms.folder.elements.files[el]).visibility !== "hidden" || 
               document.forms.folder.elements.files[el].parentNode.parentNode.style.display !== "none")
              document.forms.folder.elements.files[el].checked =
                 document.forms.folder.elements.files[el].checked == false;
               }
    }

    var uplProgr = false;

    function upload() {
    	// TODO disable all checkboxes for big volume
    	if (uplProgr)
           return false;
       document.folder.encoding = "multipart/form-data";
      
       if (document.folder.file.files) { 
          uplProgr = true;
    	   try {
    	    var xhr = new XMLHttpRequest();
    		var fd = new FormData(document.folder);
    		/* event listeners */
    		xhr.upload.addEventListener("progress", uploadProgress, false);
    		xhr.addEventListener("load", uploadComplete, false); // load end if doesn't matter success or not 
    		xhr.addEventListener("error", uploadError, false);
    		xhr.open("POST", document.folder.action, true);
    		fd.append('submit', 'Upload'); // keep consistent with view, so maybe return method in view
    		fd.append('background', '1');

    		xhr.send(fd); 
    		return false;
    	   }catch(e) {
    		   alert(e);
    	   }
       }
       return true;
    }
    
	
	function uploadProgress(evt) {
		if (evt.lengthComputable) {
			var per = (evt.loaded*100/evt.total).toFixed(0)+'%';
			getElement('_progress').innerHTML = per;
			document.title = 'Upload ('+per+')';
		}	else {
			getElement('_progress').innerHTML = evt.loaded;
		}
	}
	
	function uploadComplete(evt) {
		getElement('_progress').innerHTML = '100%';
		window.location.reload();
	}
	
	function uploadError(evt) {
		getElement('_progress').innerHTML = 'error';
               uplProgr = false;
		document.title = 'Upload error';
	}
		
	function renameFile(fid, folder) {
	   var tdel = getElement(fid);
	   var name = tdel.getAttribute('fileName');
	   getElement(fid).innerHTML='<input id="rename_input" type="text" value="'+name+'" onblur="processRename(\''+name+'\', this.value, \''+fid+'\','+folder+')">';
	   //getElement('rename_input').focus();
	}
	
	function processRename(name, newname, fid, folder) {
	    if(name == newname) {
	       restore(name, fid, folder);
	       return;
	    }
	     makeGenericAjaxCall(wf_uri+'/ajax/Rename', 'from='+encodeURIComponent(name)+'&to='+encodeURIComponent(newname)+'&path='+encodeURIComponent(document.forms.folder.path.value), true, 
	     function(res) {
	         if(res == 'ok')
	            restore(newname, fid, folder);
	         else if(res == 'oka')
	        	 restore(newname, fid, true);
	         else if(res == 'okn')
	        	 restore(newname, fid, false);
	         else
	            restore(name, fid, folder);
	     }, 
	     function (res) {
	        restore(name, fid, folder);
	     });
	}
	
	function restore(name, fid, folder) {
		// TODO if mobile flag, then add <img src=..., src can be fixed in page script variable
		// TODO folder flag can set or remove if name got .zip extension,
		// like if (!folder && endsWith('.zip') -> folder 
	    var tde = getElement(fid);
	    if (folder) {
	       tde.innerHTML='<a href="'+wf_uri+wf_path+encodeURIComponent(name)+'">'+name+'</a>';
	    } else {
	       tde.innerHTML=name; // html encoding ??
	    }
	    tde.setAttribute('fileName', name);
        // update also checkbox
	    for (var ni = 0; ni<tde.parentNode.childNodes.length; ni++) {
	    	var cn = tde.parentNode.childNodes[ni];
	    	if (cn.tagName == 'TD') {
	    		var cb = cn.firstChild;
	    		cb.value=wf_path+name;
	    		break;
	    	}
	    }
	    tde.id= 'td_'+name;
	}
  // -->
