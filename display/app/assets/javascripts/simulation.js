var host = window.location.host;
var wsUri = "ws://" + host + "/";
var websocket = {};

$(document).ready(function(){
    websocket = new WebSocket(wsUri + "startdisplay");
    websocket.onopen = function (evt) {
        console.log(evt);
    };
    websocket.onmessage = function (evt) {
        console.log(evt)
    };
});
