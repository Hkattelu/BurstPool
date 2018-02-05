var burstPool= angular.module("burstPool", ["ngRoute"]);
burstPool.config(function($routeProvider) {
    $routeProvider
    .when("/", {
        templateUrl : "burstPrice.html"
    })
    .when("/burstEarned", {
        templateUrl : "burstEarned.html"
    })
    .when("/poolUsers", {
        templateUrl : "poolUsers.html"
    })
    .when("/poolMemory", {
        templateUrl : "poolMemory.html"
    })
    .when("/poolEarned", {
        templateUrl : "poolEarned.html"
    });
});