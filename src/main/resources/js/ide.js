function retrieveEditor() {
        // Change the value and move to the secound line.
        var edit = jQuery("#ide-editor")[0];
        // Get the environment variable.
        var env = edit.bespin;
        // Get the editor.
        return env.editor;
    }

function initIDE($, pluginKey, dialog, href){

    function createTreeview(container, data) {
        function createNode(parent) {
			var current = $("<li/>").html("<span>" + this.text + "</span>").appendTo(parent);
			if (this.classes) {
				current.children("span").addClass(this.classes);
			}
			if (this.expanded) {
				current.addClass("open");
			}
			if (this.hasChildren || this.children && this.children.length) {
				var branch = $("<ul/>").appendTo(current);
				if (this.hasChildren) {
					current.addClass("hasChildren");
					createNode.call({
						text:"placeholder",
						id:"placeholder",
						children:[]
					}, branch);
				}
				if (this.children && this.children.length) {
					$.each(this.children, createNode, [branch])
				}
			}
		}
		$.each(data, createNode, [container]);
        container.treeview({add: container});
    }

    function populateBrowser() {
        function fill(tree, path) {
            var pos = path.indexOf('/');
            var children = tree.children ? tree.children : tree;
            if (pos > -1) {
                var dir = path.substring(0, pos);
                if (children.length == 0 || children[children.length - 1].text != dir) {
                    children.push({
                        text: dir,
                        expanded : false,
                        classes : "folder",
                        children : []
                    });
                }

                return fill(children[children.length - 1], path.substring(pos + 1));
            } else {
                children.push({
                    text: path,
                    classes : "file"
                });
                return children[children.length - 1];
            }
        }

        var $browser = $("#ide-browser");
        $browser.treeview();
        jQuery.get(href, function(data) {
            var tree = [], path;
            jQuery.each(data.files, function(){
                path = this;
                if (path.indexOf('/') != path.length - 1) {
                    var node = fill(tree, path);
                    if (node.text.match(/([^\/\\]+)\.(gif|jpg|jpeg)$/i)) {
                        // todo - fix the binary download REST service so we can show images in the editor - talk to Don! Seems to half work.
                        // node.text = "<a href='" + contextPath + "/rest/speakeasy/1/plugins/" + pluginKey + "/binary?path=" + path + "'>" + node.text + "</a>";
                        node.text = node.text + "";
                    }
                    else if (!node.text.match(/([^\/\\]+)\.(class)$/i)) {
                        node.text = "<a href='javascript:void(0)' id='" + path + "' class='editable-bespin'>" + node.text + "</a>";
                    }
                }
            });
            createTreeview($browser, tree);
        });
    }

    function handleBrowserFileClick(event) {
        var $target = jQuery(event.target);

        if( $target.is(".editable-bespin") ) {
            loadFile(event.target.id);
        }
    }

    function loadFile(filePath) {
        $.get(contextPath + "/rest/speakeasy/1/plugins/" + pluginKey + "/file", {path:filePath}, function(data) {
            var editor = retrieveEditor();
            editor.value = data;
            editor.fileName = filePath;

            if (filePath.match(/([^\/\\]+)\.(xml|html|js|css)$/i))
            {
                if (RegExp.$2 == 'xml')
                    editor.syntax = 'html'
                else
                    editor.syntax = RegExp.$2;
            }

            editor.setLineNumber(1);
            editor.stealFocus = true;
        });
    }

    function saveAndReload(pluginKey, fileName, contents) {
        $.ajax({
            url: contextPath + "/rest/speakeasy/1/plugins/" + pluginKey + "/file?path=" + fileName,
            data: contents,
            type: 'PUT',
            contentType: "text/plain",
            dataType: 'json',
            processData: false,
            success : function(data) {
                console.log('success');
                if (data.error) {
                    addMessage('error', {title: "Error saving extension <b>" + data.name + "</b>", body: data.error, shadowed: false});
                } else {
                    addMessage('success', {body: "<b>" + data.name + "</b> was saved successfully and reloaded", shadowed: false});
                }
            }
        })
    }

    dialog.addHeader("Edit Extension : " + pluginKey);

    dialog.addButton("Save", function (dialog) {
        var editor = retrieveEditor();
        saveAndReload(pluginKey, editor.fileName, editor.value);
        dialog.remove();
    }, "ide-save");

    // addLink not compatible with JIRA 4.2
    dialog.addButton("Cancel", function (dialog) {
        dialog.remove();
    }, "ide-cancel");

    var ideDialogContents = AJS.template.load('ide-dialog')
        .fill({
            pluginKey : pluginKey,
            "firstScript:html" : '<script src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:bespin/BespinEmbedded.js"></script>',
           })
        .toString();

    dialog.addPanel("IDE", ideDialogContents, "panel-body");

    populateBrowser();

    jQuery('#ide-browser').click(function(e) {
        handleBrowserFileClick(e);
    });

    window.onBespinLoad = function() {
        loadFile("atlassian-plugin.xml");
        $("#ide-loading").hide();
        $("#ide-editor").show();
    }

    dialog.show();
}

