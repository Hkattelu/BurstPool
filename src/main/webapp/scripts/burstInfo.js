const currencies = ["BURST", "USD", "BTC"];
var pending_burst = "19547.17";
var earned_burst = "20234.32";
var price_usd;
var price_btc;

function trim(num) {
  return Math.round(num * 100000) / 100000
}

angular.module('burstPool', []).controller(
  'InfoCtrl', function ($http, $scope, $location, $parse) {
  $scope.updatePrice = function () {
    $http.get("/getBurstPrice").then(function(response) {
      price_usd = response.data.price_usd;
      price_btc = response.data.price_btc;
      $scope.pendingBURST = trim(pending_burst);
      $scope.earnedBURST = trim(earned_burst);
      $scope.pendingUSD = trim(pending_burst * price_usd);
      $scope.earnedUSD = trim(earned_burst * price_usd);
      $scope.pendingBTC = trim(pending_burst * price_btc);
      $scope.earnedBTC = trim(earned_burst * price_btc);
    }).catch(function(err){console.log(err)})
  };

  $scope.updateMiningInfo = function () {
    $http.get("/burst", {
      params:{"requestType":"getMiningInfo"}}).
    then(function(response) {
      $parse("miningInfo").assign($scope, response.data)
    }).catch(function(err){console.log(err)})
  };

  $scope.updatePrice();
  $scope.updateMiningInfo();
  setInterval($scope.updatePrice, 10000);
  setInterval($scope.updateMiningInfo, 10000);
});

