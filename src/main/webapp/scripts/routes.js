var burstPool= angular.module("burstPool", ["ngRoute"]);
burstPool.config(function($routeProvider) {
    $routeProvider
    .when("/", {
        templateUrl : "poolUsers.html"
    })
    .when("/burstEarned", {
        templateUrl : "burstEarned.html"
    })
    .when("/poolMemory", {
        templateUrl : "poolMemory.html"
    })
    .when("/poolEarned", {
        templateUrl : "poolEarned.html"
    });
});