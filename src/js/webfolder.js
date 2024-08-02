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
    	   } catch(e) {
    		   alert(e)
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
		
	function renameFile(fid, folder,mobile) {
	   //console.log('Renaming '+fid)
	   if (getElement('rename_input'))
	        return true // event.preventDefault()
	   var tdel = getElement(fid);
	   var name = tdel.getAttribute('fileName');
	   var elem = getElement(fid)
	   mobile = !!mobile
	   if (mobile)
	      elem =elem.firstChild
	   const edith = '<input id="rename_input" type="text" value="'+escape2(name)+'" onkeydown="renameOnEnter(\''+escape3(name)+'\', this.value, \''+escape3(fid)+'\','+folder+','+mobile+')">'
	   //console.log(edith)
	   elem.innerHTML=edith
	   getElement('rename_input').focus();
	}
	
	function renameOnEnter(name, newname, fid, folder, mobile) {
		if(event.key === 'Enter') {
		    event.preventDefault()
			processRename(name, newname, fid, folder, mobile)
			return false
		} else if(event.key === "Escape") {
		    event.preventDefault()
			restore(name, fid, folder, mobile)
			return false
		}
		return true
	}
	
	function processRename(name, newname, fid, folder, mobile) {
	    if(name == newname) {
	       restore(name, fid, folder, mobile)
	       return
	    }
	    
	    makeGenericAjaxCall(wf_uri+'/ajax/Rename', 'from='+encodeURIComponent(name)+'&to='+encodeURIComponent(newname)+'&path='+encodeURIComponent(document.forms.folder.path.value), true, 
	     function(res) {
	         if(res == 'ok')
	            restore(newname, fid, folder, mobile);
	         else if(res == 'oka')
	        	 restore(newname, fid, true, mobile);
	         else if(res == 'okn')
	        	 restore(newname, fid, false, mobile);
	         else
	            restore(name, fid, folder, mobile);
	     }, 
	     function (res) {
	        restore(name, fid, folder, mobile);
	     }) 
	}
	
	function restore(name, fid, folder, mobile) {
		// TODO if mobile flag, then add <img src=..., src can be fixed in page script variable
		// TODO folder flag can set or remove if name got .zip extension,
		// like if (!folder && endsWith('.zip') -> folder 
	    var tde = getElement(fid);
	    
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
	    tde.id= 'td_'+name
	    if (mobile)
	       tde = tde.firstChild
	    if (folder) {
	       tde.innerHTML='<a href="'+wf_uri+wf_path+encodeURIComponent(name)+'">'+name+'</a>';
	    } else {
	       tde.innerHTML=escape4(name); // html encoding
	    }
	}
	
	function someName(inpc) {
	    const form1 = document.forms.folder
        var nsel = 0
        if (document.forms.folder.elements.files == undefined) {
        
        } else if (document.forms.folder.elements.files.length == undefined) {
           nsel = document.forms.folder.elements.files.checked?1:0 
        } else
           for(var el=0,n= document.forms.folder.elements.files.length; el<n; el++) {
               if (document.forms.folder.elements.files[el].checked) {
                   nsel++
                   if (nsel > 1)
                    break
               }
           }
       form1.querySelector('input[value="Edit"]').disabled = nsel != 1 && inpc.value == ''
       form1.querySelector('input[value="New folder"]').disabled = inpc.value == ''
    }
    
    function selChange(cbox) {
        if (document.forms.folder.elements.files == undefined) 
            return
        const form1 = document.forms[0]
        const named = form1.querySelector('input[name="folder"]').value != ''
        var nsel = 0
        // TODO distinct files and folders counter
        if (document.forms.folder.elements.files.length == undefined) {
            if (cbox && cbox.checked)
                nsel = 1
       } else  {
           for(var el=0,n= document.forms.folder.elements.files.length; el<n; el++) {
               if (document.forms.folder.elements.files[el].checked) {
                   nsel++
                   if (nsel > 1)
                    break
               }
           }
       }
       form1.querySelector('input[value="Edit"]').disabled = nsel != 1 && !named
       form1.querySelector('input[value="Copy"]').disabled = nsel == 0
       form1.querySelector('input[value="Delete"]').disabled = nsel == 0
       form1.querySelector('input[value="Get file links"]').disabled = nsel == 0
       form1.querySelector('input[value="Download"]').disabled = nsel == 0
    }
	
	function showMsg(msg) {
		if (!msg || msg == '') {
			getElement('status').style.visibility = 'hidden'
			return
		}
	    getElement('err_txt').innerHTML = msg
        getElement('status').style.visibility = 'visible'
    }
	
	function escape(s) {
 	   return ('' + s)
        .replace(/'/g, "\\&apos;")
    }
    
    function escape2(s) {
 	   return ('' + s)
        .replace(/&/g, "&amp;").replace(/"/g, "&quot;")
    }
    
    function escape3(s) {
 	   return ('' + s)
        .replace(/&/g, "&amp;").replace(/\\/g, "\\\\").replace(/'/g, "\\&apos;").replace(/"/g, "\\&quot;")
    }
    
    function escape4(s) {
 	   return ('' + s)
        .replace(/&/g, "&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g, "&quot;")
    }
    
  // -->
