var host = window.location.host;
var wsUri = "ws://" + host + "/";
var websocket = {};
var config = {};
var screen = {};
var states = [];
var collisions = [];
window.onbeforeunload = function () {
    websocket.onclose = function () {};
    websocket.close();
};

$(document).ready(function(){
    screen = {
        width : $("#platform").width(),
        height : $("#platform").height()
    };
    websocket = new WebSocket(wsUri + "startdisplay");
    websocket.onopen = function (evt) {
        writeToScreen("CONNECTED");
    };
    websocket.onmessage = function (evt) {
        onMessage(evt)
    };
    $("#reply").click(function () {
        function loopStates(num) {
            setTimeout(function(){
                if (states.length> num) {
                    handleState(states[num]);
                    loopStates(++num)
                }
            }, 10);
        }
        handleState(states[0]);
        loopStates(0);
    })

});

function onMessage(evt) {
    var result;
    try{
        result = JSON.parse(evt.data);
        handle(result);
    }catch(exception){
    }
    console.log(evt.data);
}
function handle(json) {
    if (json.hasOwnProperty('config')) handleConfig(json);
    if (json.hasOwnProperty('state')) {
        handleState(json);
        states.push(json)
    }
    if (json.hasOwnProperty("collision")) {
        handleCollision(json)
    }
}

function handleCollision(json) {
    for (var i = 0; i < json.collision.length; i++) {
        var collision = json.collision[i];
        var size_x = config.x+1;
        var size_y = config.y+1;
        var width = screen.width / size_x;
        var height = screen.height / size_y;

        var ctx = document.getElementById("platform")
            .getContext("2d");
        ctx.beginPath();
        ctx.arc(collision.x * width - width /2 , collision.y * height-height /2, width/2, 0, 2 * Math.PI);
        ctx.fillStyle = 'red';
        ctx.fill();
        ctx.lineWidth = 5;
        ctx.strokeStyle = '#003300';
    }
}

function handleState(json) {
    document.getElementById("platform")
        .getContext("2d")
        .clearRect(0, 0, screen.width, screen.height);
    drawMap();
    var size_x = config.x+1;
    var size_y = config.y+1;
    var width = screen.width / size_x;
    var height = screen.height / size_y;

    for (var i = 0 ; i< json.state.length; i ++ ) {
        var occupant = json.state[i];
        var centerX = occupant.x  * width + width/2;
        var centerY = occupant.y * height + height/2;
        var c = document.getElementById("platform");
        var ctx = c.getContext("2d");
        ctx.beginPath();
        ctx.arc(centerX, centerY, width/2, 0, 2 * Math.PI);
        ctx.stroke();
    }
}

function writeToScreen(message) {
    var pre = document.createElement("p");
    pre.innerHTML = message + " " + new Date();
    $("#status").append(pre);
}
function drawMap(){
    var size_x = config.x+1;
    var size_y = config.y+1;
    var width = screen.width / size_x;
    var height = screen.height / size_y;
    for(var i = 0; i < config.config.length; i++) {
        var row = config.config[i];
        for(var j = 0; j < row.length; j++) {
            if (config.config[i][j] === "Wall") {
                var c = document.getElementById("platform");
                var ctx = c.getContext("2d");
                ctx.lineWidth = 5;
                ctx.beginPath();
                ctx.rect(j * width , i * height, width, height);
                ctx.stroke();
            }
        }
    }
}
function handleConfig(json) {
    config = json;
    drawMap();
}




