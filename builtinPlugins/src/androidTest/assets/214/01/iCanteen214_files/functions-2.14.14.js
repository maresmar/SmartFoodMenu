function tick() {
    var hours, minutes, seconds, ap;
    var intHours, intMinutes, intSeconds;
    var today;
    var timeout;

    today = new Date();

    intHours = today.getHours();
    intMinutes = today.getMinutes();
    intSeconds = today.getSeconds();

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
    if (clock !== null) {
        clock.innerHTML = timeString;
    }
    window.setTimeout("tick();", 100);
}

function readKey(e) {
    var k = (navigator.appName === "Netscape") ? e : event.keyCode; // k�d stla�enej kl�vesy
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

    if (typeof console != 'undefined' && console.log) console.log('Keyboard code is: ' + code + ' stringKey is: ' + stringKey);
    var reader = document.getElementById("code");
    if (reader.value !== null && reader.value.length >= 10) {
        if (typeof console != 'undefined' && console.log) console.log('Odhlaseni bude probihat s cipem cislo: ' + reader.value);
        if ($("#reader").data('submitted') === true) {
            // Previously submitted - don't submit again
            return false;
        } else {
            // Mark it so that the next submit can be ignored
            $("#reader").data('submitted', true);
            $("#reader").submit();
            return false;
        }
//        window.location.href = document.getElementById("urlLogout").value;
    } else {
        reader.value = reader.value + stringKey;
    }

    return true;
}

function ajaxOrder(button, url, day, state) {
    if (state !== "disabled") {
        jQuery.ajax({
            url: url,
            success: function (data) {
                $('#orderContent' + day).load('db/dbJidelnicekOnDayView.jsp?day=' + day + '&terminal=' + getParameterByName('terminal') + '&printer=' + getParameterByName('printer') + '&keyboard=' + getParameterByName('keyboard') + ' #orderContent' + day + '>*', '');
                if (data.indexOf('error') !== -1) {
                    $("#container").notify("create", {
                        title: 'Stav objednávky',
                        text: 'Při zpracování objednávky došlo k chybě na serveru. Zkuste znovu načíst aplikaci a opakujte operaci.'
                    });
                } else {
                    $('#kreditInclude').html($(data).filter("#kreditInclude"));
                }
                $(".button-link").each(function () {
                    var link = $(this).attr("onclick");
                    if (link !== undefined) {
                        link = link.replace(/time=\d{13}/, "time=" + $(data).filter("#time").html());
                        $(this).attr("onclick", link);
                    }
                });
                if ($(document).width() < 600) {
                    $('.icons').hide();
                }
            }
        });
    }
}

// Ajax activity indicator bound to ajax start/stop document events
$(document).ajaxStart(function () {
    //$('#ajaxBusy').show();
    timeout = setTimeout(function() {$('#mainContext').block({ message: '<h3><img src="../../img/loading.gif"  style="vertical-align: middle;" /> Chvilku strpení...</h3>', css: { border: '1px solid' }, overlayCSS: { opacity: '0.0' }});}, 0);
}).ajaxStop(function () {
    clearTimeout(timeout);
    $('#mainContext').unblock();    
    //$('#ajaxBusy').hide();
});

function calculateHeight() {
    $("#mainContext").height($(window).height() - $("#topContext").height() - 35);
}

function calculateButtonsWidth() {
    var maxWidth = 0;
    var elemWidth = 0;
    $('.orderContent .button-link-main').each(function () {
        elemWidth = parseInt($(this).css('width'));
        if (parseInt($(this).css('width')) > maxWidth) {
            maxWidth = elemWidth;
        }
    });
    maxWidth = maxWidth + 1;
    $('.orderContent .button-link-main').each(function () {
        $(this).css('width', maxWidth + "px");
    });
}

function calculateRatings() {
    $(".rating-disabled").jRating({
        isDisabled: true,
        showRateInfo: false,
        bigStarsPath: "../../img/stars.png"
    });
    $(".rating").jRating({
        showRateInfo: false,
        bigStarsPath: "../../img/stars.png",
        phpPath: "db/rating.jsp",
        step: true,
        canRateAgain: true,
        length: 5, // nb of stars
        onSuccess: function () {
            $("#container").notify("create", {
                title: 'Hodnocení jídel',
                text: 'Vaše hodnocení jídla bylo uloženo.'
            });
        },
        onError: function () {
            $("#container").notify("create", {
                title: 'Hodnocení jídel',
                text: 'Bohužel vaše hodnocení se nepodařilo uložit.'
            });
        }
    });
}

if ($.ui !== undefined) {
    $.widget("ui.tooltip", $.ui.tooltip, {
        options: {
            content: function () {
                return $(this).prop('title');
            }
        }
    });
}

function getParameterByName(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
}