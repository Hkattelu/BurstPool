const currencies = ["BURST", "USD", "BTC"];
var pending_burst;
var earned_burst;
var price_usd;
var price_btc;

//Plot dimensions
var plotWidth = 400;
var plotHeight = 300;
var radius = Math.min(plotWidth, plotHeight) / 2;

function trim(num) {
  return Math.round(num * 100000) / 100000
}

function drawPie(percentMap, elementId) {

  // Clear old pie chart
  var canvas = d3.select('#' + elementId)
  canvas.selectAll("svg").remove();
  canvas.selectAll("g").remove();
  canvas.selectAll("path").remove()

  // Create tooltip
  var tooltipDiv = d3.select("body").append("div") 
    .attr("class", "tooltip")       
    .style("opacity", 0);

  // Create ordinal color scale
  var color = d3.scaleOrdinal(d3.schemeCategory20b);

  // Create Svg for the pie chart
  var svg = d3.select('#' + elementId)
    .append('svg')
    .attr('width', plotWidth)
    .attr('height', plotHeight)
    .append('g')
    .attr('transform', 'translate(' + (plotWidth / 2) + 
      ',' + (plotHeight / 2) + 20')');

  var arc = d3.arc().innerRadius(0).outerRadius(radius);

  var pie = d3.pie()
    .value(function(d) { return d[d3.keys(d)]*100; })
    .sort(null);

  // Create the actual pie chart, fill in color, and add on-hover tooltips
  var path = svg.selectAll('path')
    .data(pie(percentMap))
    .enter()
    .append('path')
    .attr('d', arc)
    .attr('fill', function(d, i) { 
      return color(d3.keys(d.data)[0]);
    })
    .on("mouseover", function(d) {    
      tooltipDiv.transition()    
          .duration(200)    
          .style("opacity", .9);    
      tooltipDiv .html("Account: " + d3.keys(d.data) + "<br/>Percent: " + 
        d.data[d3.keys(d.data)])  
          .style("left", (d3.event.pageX) + "px")   
          .style("top", (d3.event.pageY - 28) + "px");  
      })          
    .on("mouseout", function(d) {   
      tooltipDiv.transition()    
          .duration(500)    
          .style("opacity", 0); 
    });
}

angular.module('burstPool', []).controller(
  'InfoCtrl', function ($http, $scope, $location, $parse) {

  $scope.updatePrice = function () {
    $http.get("/pool/getBurstPrice").then(function(response) {
      price_usd = response.data.price_usd;
      price_btc = response.data.price_btc;
      if(pending_burst && earned_burst){ 
        $scope.pendingBURST = trim(pending_burst);
        $scope.earnedBURST = trim(earned_burst);
        $scope.pendingUSD = trim(pending_burst * price_usd);
        $scope.earnedUSD = trim(earned_burst * price_usd);
        $scope.pendingBTC = trim(pending_burst * price_btc);
        $scope.earnedBTC = trim(earned_burst * price_btc);
      }
    }).catch(function(err){console.log(err)})
  };

  $scope.updateMiningInfo = function () {
    $http.get("/burst", {params:{"requestType":"getMiningInfo"}}).
    then(function(response) {$parse("miningInfo").assign($scope, response.data)
    }).catch(function(err){console.log(err)})
  };

  $scope.updateLastBlockInfo = function () {
    $http.get("/burst", {params:{"requestType":"getBlock"}}).
    then(function(response) {$parse("lastBlock").assign($scope, response.data)
    }).catch(function(err){console.log(err)})
  };

  $scope.updateStatistics = function () {
    $http.get("/pool/statistics").then(function(response) {
      $parse("stats").assign($scope, response.data)
    }).catch(function(err){console.log(err)})
  };

  $scope.updateCurrentShares = function () {
    $http.get("/pool/shares/current").then(function(response) {
      drawPie(response.data, "currentShares")
    }).catch(function(err){console.log(err)})
  };

  $scope.updateHistoricShares = function () {
    $http.get("/pool/shares/historic").then(function(response) {
      drawPie(response.data, "historicShares")
    }).catch(function(err){console.log(err)})
  };

  $scope.updatePrice();
  $scope.updateMiningInfo();
  $scope.updateLastBlockInfo();
  $scope.updateStatistics();
  $scope.updateCurrentShares();
  $scope.updateHistoricShares();

  setInterval($scope.updatePrice, 5000); // Every 5 seconds
  setInterval($scope.updateMiningInfo, 5000); // Every 5 seconds
  setInterval($scope.updateLastBlockInfo, 5000); // Every 5 seconds
  setInterval($scope.updateCurrentShares, 20000); // Every 5 seconds
  setInterval($scope.updateHistoricShares, 5000); // Every 5 seconds
  setInterval($scope.updateStatistics, 60000); // Every minute
});