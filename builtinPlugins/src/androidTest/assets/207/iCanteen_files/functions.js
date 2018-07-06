function bgset(tr) {
    if (tr.style.background=='#f7e794' || tr.style.background.indexOf('rgb(240, 240, 240)')!=-1)
        tr.style.background='#FFFFCC'; //D3DCE3
    else
        tr.style.background='#f7e794';
}

function bgunset(tr) {
    tr.style.background='#FFFFCC';
}

function tick() {
    var hours, minutes, seconds, ap;
    var intHours, intMinutes, intSeconds;
    var today;

    today = new Date();

    intHours = today.getHours();
    intMinutes = today.getMinutes();
    intSeconds = today.getSeconds();

    intHours = intHours
    hours = intHours + ":";

    if (intMinutes < 10) {
        minutes = "0" + intMinutes + ":";
    } else {
        minutes = intMinutes + ":";
    }

    if (intSeconds < 10) {
        seconds = "0" + intSeconds + " ";
    } else {
        seconds = intSeconds + " ";
    }
    timeString = hours + minutes + seconds;
    clock = document.getElementById("Clock");
    if (clock != null ) {
        clock.innerHTML = timeString;
    }
    window.setTimeout("tick();", 100);
}

function readKey(e){
    var k = (navigator.appName=="Netscape") ? e : event.keyCode; // k�d stla�enej kl�vesy
    var stringKey;

    switch (k) {
        case 48:
            stringKey = "0";
            break;
        case 49:
            stringKey = "1";
            break;
        case 50:
            stringKey = "2";
            break;
        case 51:
            stringKey = "3";
            break;
        case 52:
            stringKey = "4";
            break;
        case 53:
            stringKey = "5";
            break;
        case 54:
            stringKey = "6";
            break;
        case 55:
            stringKey = "7";
            break;
        case 56:
            stringKey = "8";
            break;
        case 57:
            stringKey = "9";
            break;
        case 96:
            stringKey = "0";
            break;
        case 97:
            stringKey = "1";
            break;
        case 98:
            stringKey = "2";
            break;
        case 99:
            stringKey = "3";
            break;
        case 100:
            stringKey = "4";
            break;
        case 101:
            stringKey = "5";
            break;
        case 102:
            stringKey = "6";
            break;
        case 103:
            stringKey = "7";
            break;
        case 104:
            stringKey = "8";
            break;
        case 105:
            stringKey = "9";
            break;
        case 65:
            stringKey = "A";
            break;
        case 66:
            stringKey = "B";
            break;
        case 67:
            stringKey = "C";
            break;
        case 68:
            stringKey = "D";
            break;
        case 69:
            stringKey = "E";
            break;
        case 70:
            stringKey = "F";
            break;
        default:
            stringKey = "";
            break;
    }

    //alert("keyboard code is: "+k+" stringKey is: "+stringKey);
    var reader = document.getElementById("code");
    if (reader.value.length==10) {
        window.location.href=document.getElementById("urlLogout").value;
    } else {
        reader.value=reader.value+stringKey;
    }

    return true;
}

/***********************************************
                 * Fixed ToolTip script- © Dynamic Drive (www.dynamicdrive.com)
                 * This notice MUST stay intact for legal use
                 * Visit http://www.dynamicdrive.com/ for full source code
                 ***********************************************/

var tipwidth='150px' //default tooltip width
var tipbgcolor='lightyellow'  //tooltip bgcolor
var disappeardelay=250  //tooltip disappear speed onmouseout (in miliseconds)
var vertical_offset="0px" //horizontal offset of tooltip from anchor link
var horizontal_offset="-3px" //horizontal offset of tooltip from anchor link

/////No further editting needed

var ie4=document.all
var ns6=document.getElementById&&!document.all

if (ie4||ns6)
    document.write('<div id="fixedtipdiv" class="noPrint" style="visibility:hidden;width:'+tipwidth+';background-color:'+tipbgcolor+'" ></div>')

function getposOffset(what, offsettype){
    var totaloffset=(offsettype=="left")? what.offsetLeft : what.offsetTop;
    var parentEl=what.offsetParent;
    while (parentEl!=null){
        totaloffset=(offsettype=="left")? totaloffset+parentEl.offsetLeft : totaloffset+parentEl.offsetTop;
        parentEl=parentEl.offsetParent;
    }
    return totaloffset;
}


function showhide(obj, e, visible, hidden, tipwidth){
    if (ie4||ns6)
        dropmenuobj.style.left=dropmenuobj.style.top=-500
    if (tipwidth!=""){
        dropmenuobj.widthobj=dropmenuobj.style
        dropmenuobj.widthobj.width=tipwidth
    }
    if (e.type=="click" && obj.visibility==hidden || e.type=="mouseover")
        obj.visibility=visible
    else if (e.type=="click")
        obj.visibility=hidden
}

function iecompattest(){
    return (document.compatMode && document.compatMode!="BackCompat")? document.documentElement : document.body
}

function clearbrowseredge(obj, whichedge){
    var edgeoffset=(whichedge=="rightedge")? parseInt(horizontal_offset)*-1 : parseInt(vertical_offset)*-1
    if (whichedge=="rightedge"){
        var windowedge=ie4 && !window.opera? iecompattest().scrollLeft+iecompattest().clientWidth-15 : window.pageXOffset+window.innerWidth-15
        dropmenuobj.contentmeasure=dropmenuobj.offsetWidth
        if (windowedge-dropmenuobj.x < dropmenuobj.contentmeasure)
            edgeoffset=dropmenuobj.contentmeasure-obj.offsetWidth
    }
    else{
        var windowedge=ie4 && !window.opera? iecompattest().scrollTop+iecompattest().clientHeight-15 : window.pageYOffset+window.innerHeight-18
        dropmenuobj.contentmeasure=dropmenuobj.offsetHeight
        if (windowedge-dropmenuobj.y < dropmenuobj.contentmeasure)
            edgeoffset=dropmenuobj.contentmeasure+obj.offsetHeight
    }
    return edgeoffset
}

function fixedtooltip(menucontents, obj, e, tipwidth){
    if (window.event) event.cancelBubble=true
    else if (e.stopPropagation) e.stopPropagation()
    clearhidetip()
    dropmenuobj=document.getElementById? document.getElementById("fixedtipdiv") : fixedtipdiv
    dropmenuobj.innerHTML=menucontents

    if (ie4||ns6){
        showhide(dropmenuobj.style, e, "visible", "hidden", tipwidth)
        dropmenuobj.x=getposOffset(obj, "left")
        dropmenuobj.y=getposOffset(obj, "top")
        dropmenuobj.style.left=dropmenuobj.x-clearbrowseredge(obj, "rightedge")+"px"
        dropmenuobj.style.top=dropmenuobj.y-clearbrowseredge(obj, "bottomedge")+obj.offsetHeight+"px"
    }
}

function hidetip(e){
    if (typeof dropmenuobj!="undefined"){
        if (ie4||ns6)
            dropmenuobj.style.visibility="hidden"
    }
}

function delayhidetip(){
    if (ie4||ns6)
        delayhide=setTimeout("hidetip()",disappeardelay)
}

function clearhidetip(){
    if (typeof delayhide!="undefined")
        clearTimeout(delayhide)
}

function ajaxOrder(button, url, day) {
    button.disabled = true;
    jQuery.ajax({
        url: url,
        success: function(data) {       
            $('#orderContent'+day).load('db/dbJidelnicekOnDay.jsp?day='+ day + ' #orderContent'+day+'>*','');
            if (data.indexOf('fail') !== -1) {  
                alert(data);
            } else {
                $('#kreditInclude').html($(data).filter("#kreditInclude"));
            }
            button.disabled = false;
            
            $(":button").each(function() {
                var link = $(this).attr("onclick");
                link = link.replace(/time=\d{13}/, "time="+$(data).filter("#time").html());
                $(this).attr("onclick", link); 
            });
        },
        error: function() {
            button.disabled = false;
            alert('Při zpracování objednávky došlo k chybě na serveru.');
        }
    });
}