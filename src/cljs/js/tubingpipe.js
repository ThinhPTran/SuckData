// goog.provide('tubingpipe');

/* Tubing Pipe draws a tubing pipe diagram using D3.js with the specified parameters*/

// options map
// {
//   height: int (in pixels)
//   width: int (in pixels)
//   max-depth: int
//   valves: [{
//     depth: int,
//     category: string,
//     pct-open: int/float,
//     status: string,
//   }],
//   regimes: [[end, regime], ...]
//   animations-enabled: boolean
// }

var tubingpipe = function(targetNode, options){
  var width = 130,
      l_padding = 20,
      r_padding = 20,
      t_padding = 80,
      b_padding = 50; // HACK: match starting point of dvsp highchart chart

  // // Fake data for now
  // var regimes = [
  // // { start: 0,
  // //   end: 2500,
  // //   type: "bubble"
  // // },
  // // { start: 2500,
  // //   end: 5000,
  // //   type: "slug"
  // // },
  // // { start: 5000,
  // //   end: 7000,
  // //   type: "annular"
  // // },
  // { start: 0,
  //   end: 9000,
  //   type: "churn"}];

  // Initialize scale
  var yScale = d3.scale.linear()
    .domain([0, options["max-depth"]])
    .range([t_padding, options["height"] - b_padding]);

  //Clear targetNode
  d3.select(targetNode).selectAll("*").remove();

  // Insert svg into DOM
  var svg = d3.select(targetNode)
    .append("svg")
    .attr({
      width: options["width"],
      height: options["height"]
    });


  // Defs
  var defs = svg.append("defs");

  // Color gradient for bubble regime
  var gradient = defs.append("radialGradient")
    .attr("id", "bubbleRegimeGradient")
    .attr("cx", 0.35)
    .attr("cy", 0.35)
    .attr("r", 0.75);
  gradient.append("stop")
    .attr("class", "gas-secondary-stop")
    .attr("offset", "0%")
  gradient.append("stop")
    .attr("class", "gas-primary-stop")
    .attr("offset", "100%")

  // Color gradient for annular regime
  var annularGradient = defs.append("linearGradient")
    .attr("id", "annularRegimeGradient")
  annularGradient.append("stop")
    .attr("offset", "5%")
    .attr("class", "gas-primary-stop");
  annularGradient.append("stop")
    .attr("offset", "40%")
    .attr("class", "gas-secondary-stop");
  annularGradient.append("stop")
    .attr("offset", "95%")
    .attr("class", "gas-primary-stop");

  // Simulate curvature of tube with color gradient
  var tubingColorGradient = defs.append("linearGradient")
  .attr("id", "tubingColorGradient");
  tubingColorGradient.append("stop")
    .attr("offset", "5%")
    .attr("class", "tubing-pipe-secondary-stop");
  tubingColorGradient.append("stop")
    .attr("offset", "50%")
    .attr("class", "tubing-pipe-primary-stop");
  tubingColorGradient.append("stop")
    .attr("offset", "95%")
    .attr("class", "tubing-pipe-secondary-stop");

  //
  var gasRegimeGradient = defs.append("linearGradient")
    .attr("id", "gasRegimeGradient");
    gasRegimeGradient.append("stop")
      .attr("offset", "5%")
      .attr("class", "tubing-pipe-gas-secondary-stop");
    gasRegimeGradient.append("stop")
      .attr("offset", "50%")
      .attr("class", "tubing-pipe-gas-primary-stop");
    gasRegimeGradient.append("stop")
      .attr("offset", "95%")
      .attr("class", "tubing-pipe-gas-secondary-stop");

  var liquidRegimeGradient = defs.append("linearGradient")
    .attr("id", "liquidRegimeGradient");
    liquidRegimeGradient.append("stop")
      .attr("offset", "5%")
      .attr("class", "tubing-pipe-liquid-secondary-stop");
    liquidRegimeGradient.append("stop")
      .attr("offset", "50%")
      .attr("class", "tubing-pipe-liquid-primary-stop");
    liquidRegimeGradient.append("stop")
      .attr("offset", "95%")
      .attr("class", "tubing-pipe-liquid-secondary-stop");

  // Gradient to fade the regimes near their transitions
  var maskGradient = defs.append("linearGradient")
      .attr("id", "regimeMaskGradient")
      .attr("x1", "0%")
      .attr("y1", "0%")
      .attr("x2", "0%")
      .attr("y2", "100%");
    maskGradient.append("stop")
      .attr("offset", "0%")
      .attr("stop-color", "black");
    maskGradient.append("stop")
      .attr("offset", "15%")
      .attr("stop-color", "white");
    maskGradient.append("stop")
      .attr("offset", "85%")
      .attr("stop-color", "white");
    maskGradient.append("stop")
      .attr("offset", "100%")
      .attr("stop-color", "black");

  // Goo filter used for churn flow regime
  var gooey = defs.append("filter")
    .attr("id", "gooFilter")
    gooey.append("feGaussianBlur")
      .attr("in", "SourceGraphic")
      .attr("stdDeviation", 7)
      .attr("result", "blur");
    gooey.append("feColorMatrix")
      .attr("in", "blur")
      .attr("mode", "matrix")
      .attr("values", "1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 18 -7")
      .attr("result", "goo");

  var dropShadow = defs.append("filter")
    .attr("id", "dropShadowFilter")
    .attr("x", 0)
    .attr("y", 0)
    .attr("width", "200%")
    .attr("height", "200%");
    dropShadow.append("feOffset")
      .attr("result", "offOut")
      .attr("in", "SourceAlpha")
      .attr("dx", 5)
      .attr("dy", 5);
    dropShadow.append("feGaussianBlur")
      .attr("result", "blurOut")
      .attr("in", "offOut")
      .attr("stdDeviation", 10);
    dropShadow.append("feBlend")
      .attr("in", "SourceGraphic")
      .attr("in2", "blurOut")
      .attr("mode", "normal");


  // Gas/Liquid Regimes
  // These regimes are graphically identical except for color
  var drawSolidRegime = function(target, x, y, width, height, animate, fill){
    target.append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .attr("fill", fill);
  }

  var drawGas = function(target, x, y, width, height, animate){
    var group = d3.select(target).append("g");

    drawSolidRegime(group, x, y, width, height, animate, "url(#gasRegimeGradient)");

    //ToolTip hitbox
    group.append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .style("fill-opacity", "0.0")
      .on("mouseover", function(){
        tooltip.style("left", (x+50) + "px")
          .style("top", (y+30) + "px")
          .selectAll("div")
          .remove();

        var content = tooltip.append("div");
        content.append("h3").text("Gas");
        //content.append("p").text("Annular flow is a flow regime of two-phase gas-liquid flow (see gas-liquid flow). It is characterized by the presence of a liquid film flowing on the channel wall (in a round channel this film is annulus-shaped which gives the name to this type of flow) and with the gas flowing in the gas core.");

        tooltip.classed("hidden", false);
      })
      .on("mouseleave", function(){
        tooltip.classed("hidden", true);
      });
  }

  var drawLiquid = function(target, x, y, width, height, animate){
    var group = d3.select(target).append("g");

    drawSolidRegime(group, x, y, width, height, animate, "url(#liquidRegimeGradient)");

    //ToolTip hitbox
    group.append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .style("fill-opacity", "0.0")
      .on("mouseover", function(){
        tooltip.style("left", (x+50) + "px")
          .style("top", (y+30) + "px")
          .selectAll("div")
          .remove();

        var content = tooltip.append("div");
        content.append("h3").text("Liquid");
        //content.append("p").text("Annular flow is a flow regime of two-phase gas-liquid flow (see gas-liquid flow). It is characterized by the presence of a liquid film flowing on the channel wall (in a round channel this film is annulus-shaped which gives the name to this type of flow) and with the gas flowing in the gas core.");

        tooltip.classed("hidden", false);
      })
      .on("mouseleave", function(){
        tooltip.classed("hidden", true);
      });
  }

  // Regimes
  var drawBubbleRegime = function(target, x, y, width, height, animate) {
    drawLiquid(target, x, y, width, height, animate);
    var numBubbles = Math.floor((width * height) / 200);
    var group = d3.select(target).append("g");

    var clipPathID = "bubbleRegimeClip" + Math.random();
    var clipPath = group.append("clipPath")
      .attr("id", clipPathID)
      .append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height);

    var maskID = "bubbleRegimeMask" + Math.random();
    var mask = group.append("mask")
      .attr("id", maskID)
      .append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .attr("fill", "url(#regimeMaskGradient)");

    var circleGroup = group.append("g")
    .attr("clip-path", "url(#" + clipPathID + ")")
    .attr("mask", "url(#" + maskID + ")")

    for(var i = 0; i < numBubbles; i++){
      var c_x = Math.floor(Math.random() * width) + x;
      var c_y = Math.floor(Math.random() * height) + y;
      var radius = Math.floor(Math.random() * 3) + 3 //radius between 3-6px

      var circle1 = circleGroup.append("circle")
        .attr("cx", c_x)
        .attr("cy", c_y)
        .attr("r", radius)
        .attr("fill", "url(#bubbleRegimeGradient)")

      if (animate){
        //2nd copy of circle mirrored below the clippath for the animation
        var circle2 = circleGroup.append("circle")
          .attr("cx", c_x)
          .attr("cy", c_y + height)
          .attr("r", radius)
          .attr("fill", "url(#bubbleRegimeGradient)")

        var animDur = Math.floor(Math.random() * 5) + 5;
        circle1.append("animate")
          .attr("attributeName", "cy")
          .attr("attributeType", "XML")
          .attr("from", c_y)
          .attr("to", c_y - height)
          .attr("dur", animDur + "s")
          .attr("repeatCount", "indefinite");

        circle2.append("animate")
          .attr("attributeName", "cy")
          .attr("attributeType", "XML")
          .attr("from", c_y + height)
          .attr("to",  c_y)
          .attr("dur", animDur + "s")
          .attr("repeatCount", "indefinite");
      }
    }

    //ToolTip hitbox
    group.append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .style("fill-opacity", "0.0") //transparent
      .on("mouseover", function(){
        tooltip.style("left", (x+50) + "px")
          .style("top", (y+30) + "px")
          .selectAll("div")
          .remove();

        var content = tooltip.append("div");
        content.append("h3").text("Bubble Flow");
        content.append("p").text("A multiphase fluid flow regime characterized by the gas phase being distributed as bubbles through the liquid phase.");

        tooltip.classed("hidden", false);
      })
      .on("mouseleave", function(){
        tooltip.classed("hidden", true);
      });
  }

  var drawSlugRegime = function(target, x, y, width, height, animate) {
    drawLiquid(target, x, y, width, height, animate);
    var spacing = 10; //spacing between slugs in px
    var group = d3.select(target).append("g");

    var clipPathID = "SlugRegimeClip" + Math.random();
    var clipPath = group.append("clipPath")
      .attr("id", clipPathID)
      .append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height);

    var maskID = "SlugRegimeMask" + Math.random();
    var mask = group.append("mask")
      .attr("id", maskID)
      .append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .attr("fill", "url(#regimeMaskGradient)");

    var pos = 0;
    var v_size = Math.floor(10 + (width * 1.5)); // 40-50px high slugs
    var s_height = height + (height % (v_size + spacing)); //Total height of the slugs sequence
    while(pos < height){
      var c_x = x + (width/2);
      var r_x = (width/2) + 10; //make them fat

      var c_y = y + pos + (v_size/2);
      var r_y = (v_size/2);

      pos = pos + v_size + spacing; //update current pos

      var slug1 = group.append("ellipse")
        .attr("cx", c_x)
        .attr("cy", c_y)
        .attr("ry", r_y)
        .attr("rx", r_x)
        .attr("fill", "url(#bubbleRegimeGradient)")
        .attr("clip-path", "url(#" + clipPathID + ")")
        .attr("mask", "url(#" + maskID + ")");

      //2nd copy of circle mirrored below the clippath for the animation
      if (animate){
        var slug2 = group.append("ellipse")
          .attr("cx", c_x)
          .attr("cy", c_y + s_height)
          .attr("ry", r_y)
          .attr("rx", r_x)
          .attr("fill", "url(#bubbleRegimeGradient)")
          .attr("clip-path", "url(#" + clipPathID + ")")
          .attr("mask", "url(#" + maskID + ")");

        slug1.append("animate")
          .attr("attributeName", "cy")
          .attr("attributeType", "XML")
          .attr("from", c_y)
          .attr("to", c_y - s_height)
          .attr("dur", "10s")
          .attr("repeatCount", "indefinite");

        slug2.append("animate")
          .attr("attributeName", "cy")
          .attr("attributeType", "XML")
          .attr("from", c_y + s_height)
          .attr("to",  c_y)
          .attr("dur", "10s")
          .attr("repeatCount", "indefinite");
      }
    }

    //ToolTip hitbox
    group.append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .style("fill-opacity", "0.0")
      .on("mouseover", function(){
        tooltip.style("left", (x+50) + "px")
          .style("top", (y+30) + "px")
          .selectAll("div")
          .remove();

        var content = tooltip.append("div");
        content.append("h3").text("Slug Flow");
        content.append("p").text("Slug flow is a liquid–gas two-phase flow in which the gas phase exists as large bubbles separated by liquid \"slugs\". Pressure oscillations within piping can be caused by slug flow");

        tooltip.classed("hidden", false);
      })
      .on("mouseleave", function(){
        tooltip.classed("hidden", true);
      });
  }

  var drawAnnularRegime = function(target, x, y, width, height, animate){
    drawLiquid(target, x, y, width, height, animate);
    var group = d3.select(target).append("g");

    var clipPathID = "annularRegimeClip" + Math.random();
    var clipPath = group.append("clipPath")
      .attr("id", clipPathID)
      .append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height);

    var maskID = "annularRegimeMask" + Math.random();
    var mask = group.append("mask")
      .attr("id", maskID)
      .append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .attr("fill", "url(#regimeMaskGradient)");

    group.attr("clip-path", "url(#" + clipPathID + ")")
      .attr("mask", "url(#" + maskID + ")");

    var motionID = "annularMotionID" + Math.random();
    var motion = "M0,0 l0,-" + height;
    var motionPath = group.append("path")
      .attr("id", motionID)
      .attr("d", motion);

    //Make the path
    var path = "M" + (x+5) + " " + y + "q -5,5 0,10";
    var pos = 10;
    //First go down
    while(pos < (2*height)){
      pos = pos + 10;
      path = path + "t 0,10";
    }
    //Then over
    path = path + "l" + (width-10) + ",0";

    //Then up
    path = path + "q-5,-5 0,-10";
    pos = pos - 10;
    while(pos > 0){
      pos = pos - 10;
      path = path + "t 0,-10"
    }
    //Then close it
    path = path + "Z";

    var flow = group.append("path")
      .attr("d", path)
      .attr("fill", "url(#annularRegimeGradient)");

    if (animate){
      flow.append("animateMotion")
        .attr("dur", "10s")
        .attr("repeatCount", "indefinite")
        .append("mpath")
        .attr("xlink:href", "#" + motionID);
    }

    //ToolTip hitbox
    group.append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .style("fill-opacity", "0.0")
      .on("mouseover", function(){
        tooltip.style("left", (x+50) + "px")
          .style("top", (y+30) + "px")
          .selectAll("div")
          .remove();

        var content = tooltip.append("div");
        content.append("h3").text("Annular Flow");
        content.append("p").text("Annular flow is a flow regime of two-phase gas-liquid flow (see gas-liquid flow). It is characterized by the presence of a liquid film flowing on the channel wall (in a round channel this film is annulus-shaped which gives the name to this type of flow) and with the gas flowing in the gas core.");

        tooltip.classed("hidden", false);
      })
      .on("mouseleave", function(){
        tooltip.classed("hidden", true);
      });
  }

  var drawChurnRegime = function(target, x, y, width, height, animate) {
    drawLiquid(target, x, y, width, height, animate);
    // Pretty much done the same way as the bubble regime except with
    // some filters applied to make the goo effect
    var numBubbles = Math.floor((width * height) / 300);
    var group = d3.select(target).append("g");

    var clipPathID = "churnRegimeClip" + Math.random();
    var clipPath = group.append("clipPath")
      .attr("id", clipPathID)
      .append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height);

    var maskID = "churnRegimeMask" + Math.random();
    var mask = group.append("mask")
      .attr("id", maskID)
      .append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .attr("fill", "url(#regimeMaskGradient)");

    var circleGroup = group.append("g")
    .attr("clip-path", "url(#" + clipPathID + ")")
    .attr("mask", "url(#" + maskID + ")")
    .attr("filter", "url(#gooFilter)");

    for(var i = 0; i < numBubbles; i++){
      var c_x = Math.floor(Math.random() * width) + x;
      var c_y = Math.floor(Math.random() * height) + y;
      var radius = Math.floor(Math.random() * 3) + 7 //radius between 3-5px

      var circle1 = circleGroup.append("circle")
        .attr("cx", c_x)
        .attr("cy", c_y)
        .attr("r", radius)
        .attr("fill", "url(#bubbleRegimeGradient)")

      //2nd copy of circle mirrored below the clippath for the animation
      if (animate){
        var circle2 = circleGroup.append("circle")
          .attr("cx", c_x)
          .attr("cy", c_y + height)
          .attr("r", radius)
          .attr("fill", "url(#bubbleRegimeGradient)")

        var animDur = Math.floor(Math.random() * 3) + 7;
        circle1.append("animate")
          .attr("attributeName", "cy")
          .attr("attributeType", "XML")
          .attr("from", c_y)
          .attr("to", c_y - height)
          .attr("dur", animDur + "s")
          .attr("repeatCount", "indefinite");

        circle2.append("animate")
          .attr("attributeName", "cy")
          .attr("attributeType", "XML")
          .attr("from", c_y + height)
          .attr("to",  c_y)
          .attr("dur", animDur + "s")
          .attr("repeatCount", "indefinite");
      }
    }

    //ToolTip hitbox
    group.append("rect")
      .attr("x", x)
      .attr("y", y)
      .attr("width", width)
      .attr("height", height)
      .style("fill-opacity", "0.0")
      .on("mouseover", function(){
        tooltip.style("left", (x+50) + "px")
          .style("top", (y+30) + "px")
          .selectAll("div")
          .remove();

        var content = tooltip.append("div");
        content.append("h3").text("Churn Flow");
        content.append("p").text("Churn Flow is a highly disturbed flow of gas and liquid. It is characterized by the presence of a very thick and unstable liquid film, with the liquid often oscillating up and down.");

        tooltip.classed("hidden", false);
      })
      .on("mouseleave", function(){
        tooltip.classed("hidden", true);
      });
  }

  // Valve
  // (x,y) is the center of the injection point
  // valveData is a map of the following format
  // {depth: number,
  //  category: string,
  //  pct-open: number,
  //  status: string}
  var drawValve = function(target, x, y, valve){
    group = d3.select(target).append("g");

    var points = [
      [x, y-5], [x, y+5], [x+25, y+5], [x+25, y-10], [x+15,y-10], [x+15, y-5]];

    var pointsString = "";
    for(var i = 0; i < points.length; i++){
      pointsString = pointsString + points[i][0] + "," + points[i][1] + " ";
    }

    // valve tube
    group.append("polygon")
      .attr("points", pointsString)
      .attr("class", "valveTube");

    // rect on top valve tube
    group.append("rect")
      .attr("x", x+5)
      .attr("y", y-40)
      .attr("width", 30)
      .attr("height", 30)
      .attr("class", "valveBox");

    // Draw a status circle instead if pct-open is not specified but status is
    if (valve["status"] && (valve["status"] !== 'use-pct-open')){
      // status circle
      statusCircle = group.append("circle")
        .attr("cx", x+20)
        .attr("cy", y-25)
        .attr("r", 10)

      if(valve["status"] == "open"){
        statusCircle.attr("class", "valve-open");
      }else if(valve["status"] == "closed"){
        statusCircle.attr("class", "valve-closed");
      }else if(valve["status"] == "back-check"){
        statusCircle.attr("class", "valve-back-check");
      }else if(valve["status"] == "is-dummy"){
        statusCircle.attr("class", "valve-dummy");
      }else if(valve["status"] == "transition"){
        statusCircle.attr("class", "valve-transition");
      }else if(valve["status"] == "unknown"){
        statusCircle.attr("class", "valve-unknown");
      }
    }
    if ((typeof valve["pct-open"] !== 'undefined')
        && (valve["status"] == "use-pct-open")){
      // Valve percent open indicator
      // First make a scale to map the % open to a color between red and green
      var colors = ["#E82525", "#E86625", "#E88D25", "#E8CE25", "#CEE825",
                    "#A1E825", "#5CE825", "#2CE825"];

      var heatmapColor = d3.scale.linear()
        .domain(d3.range(0, 1, 1.0 / (colors.length - 1)))
        .range(colors);

      var c = d3.scale.linear().domain([0, 100]).range([0,1]);

      // Define the path the indicator will be drawn in
      var arc = d3.svg.arc()
        .innerRadius(10)
        .outerRadius(12)
        .startAngle(0) // (5*pi)/4 // 7:30 position
        .endAngle(((2*Math.PI) * (c(valve["pct-open"]))));

      group.append("path")
        .style("fill", heatmapColor(c(valve["pct-open"])))
        .attr("d", arc)
        .attr("transform", "translate(" + (x+20) + "," + (y-25) + ")");

      group.append("text")
        .attr("x", x+20)
        .attr("y", y-22)
        .attr("class", "valveIndicatorText")
        .style("fill", heatmapColor(c(valve["pct-open"])))
        .text(valve["pct-open"].toString());
    }

    //ToolTip hitbox
    group.append("rect")
      .attr("x", x)
      .attr("y", y-40)
      .attr("width", 50)
      .attr("height", 50)
      .style("fill-opacity", "0.0")
      .on("mouseover", function(){
        tooltip.style("left", (x + 60) + "px")
          .style("top", (y-40) + "px")
          .selectAll("div")
          .remove();

        var content = tooltip.append("div");
        content.append("p").text("Depth: " + valve['depth'].toFixed(2) + " ft");
        if (valve['category']){
          content.append("p").text("Category: " + valve['category']);}
        if (valve['status']){
          if (valve['status'] !== "use-pct-open"){
            content.append("p").text("Status: " + valve['status']);}
          else if((typeof valve["pct-open"] !== 'undefined')){
            content.append("p").text("% Open: " + valve['pct-open']);}
        }

        tooltip.classed("hidden", false);
      })
      .on("mouseleave", function(){
        tooltip.classed("hidden", true);
      });
  }

  // Setup Tooltip. made as a div that is repositioned and
  // shown/hidden on mouse hovers
  var tooltip = d3.select(targetNode)
    .append("div")
    .attr("id", "tubingpipetooltip")
    .attr("class", "hidden");

  // Draw Background
  svg.append("rect")
    .attr("width","100%")
    .attr("height","100%")
    .attr("fill-opacity", "0.0");

//  svg.append("g")
//      .attr("class", "axis")
//      .attr("transform", "translate(" + (l_padding + 150) + ",0)")
//      .call(yAxis);

  // Draw Tube
  // 2 Rectangles, one on top the other
  // The bottom layer one will look like the tube casing
  var tube = svg.append("rect")
    .attr({height: options["height"] - (t_padding + b_padding),
           width: 50,
           transform: "translate(" + l_padding + "," + t_padding + ")"})
    .attr("class", "tubing-border");

  var InnerTube = svg.append("rect")
    .attr({height: options["height"] - (t_padding + b_padding),
           width: 40,
           transform: "translate(" + (l_padding + 5) + "," + t_padding + ")"})
    .attr("fill", "url(#tubingColorGradient)");

    // .append("polygon")
    // .attr("class", "valve")
    // .attr("points", "0,-30 20,-20 20,20 0,30")
    // .attr("transform", function(d){
    //   return ("translate(" + 100 + "," + yScale(d.measDepth) + ")");})
    // .attr("x", function(d){return 100;})
    // .attr("y", function(d){return yScale(d.measDepth);})
    // .on("mouseover", function(d, i){
    //   var xPosition = parseFloat(d3.select(this).attr("x"));
    //   var yPosition = parseFloat(d3.select(this).attr("y"));

    //   tooltip.style("left", (xPosition + 30) + "px")
    //          .style("top", (yPosition - 20) + "px")
    //          .selectAll("p")
    //          .remove();

    //   var content = tooltip.append("div");
    //   content.append("p").text("Mandrel " + d.id);
    //   content.append("p").text("Measured Depth: " + d.measDepth);
    //   content.append("p").text("Category: " + d.valveCategory)
    //   content.append("p").text("Status: " + d.valveStatus);

    //   tooltip.classed("hidden", false);
    // })
    // .on("mouseleave", function(){
    //   tooltip.classed("hidden", true);
    // });

  // Draw Regimes
  var lastEnd = 0;
  var regimes = svg.selectAll(".regime")
    .data(options["regimes"])
    // .sort(function(a, b){return d3.descending(a[0], b[0]);})
    .enter()
    .append("g")
    .each(function(d, i){
      var type = d[1],
      end = d[0],
      y_start = yScale(lastEnd),
      x_start = 5 + l_padding,
      height = yScale(end) - y_start,
      animate = options["animations-enabled"],
      width = 40;
      if(type == "bubble"){
        drawBubbleRegime(this, 5 + l_padding, y_start, width, height, animate);
      }
      else if(type == "slug"){
        drawSlugRegime(this, 5 + l_padding, y_start, width, height, animate);
      }
      else if(type == "annular"){
        drawAnnularRegime(this, 5 + l_padding, y_start, width, height, animate);
      }
      else if(type == "churn"){
        drawChurnRegime(this, 5 + l_padding, y_start, width, height, animate);
      }
      else if(type == "gas"){
        drawGas(this, 5 + l_padding, y_start, width, height, animate);
      }
      else if(type == "liquid"){
        drawLiquid(this, 5 + l_padding, y_start, width, height, animate);
      }
      lastEnd = d[0];
    });

  // Draw Valves
  var valves = svg.selectAll(".valve")
    .data(options["valves"])
    .enter()
    .append("g")
    .each(function(d, i){
      drawValve(this, l_padding + 50, yScale(d['depth']), d);
    });
}
