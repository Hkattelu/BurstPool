const currencies = ["BURST", "USD", "BTC"];
var pending_burst = "19547.17";
var earned_burst = "20234.32";
var price_usd;
var price_btc;

function trim(num) {
  return Math.round(num * 100000) / 100000
}

function updatePrice() {
  $.get(location.origin + "/getBurstPrice", function(response){
    console.log(response)
    price_usd = response.price_usd;
    price_btc = response.price_btc;
    $("#pending-BURST").html(trim(pending_burst));
    $("#earned-BURST").html(trim(earned_burst));
    $("#pending-USD").html(trim(pending_burst * price_usd));
    $("#earned-USD").html(trim(earned_burst * price_usd));
    $("#pending-BTC").html(trim(pending_burst * price_btc));
    $("#earned-BTC").html(trim(earned_burst * price_btc));
  });
}

updatePrice();
// Update the price every 10 seconds
setInterval(updatePrice, 10000);

