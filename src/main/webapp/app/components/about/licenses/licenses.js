(function() {
    'use strict';

    angular.module('lavagna.components').component('lvgAboutLicenses', {
    	templateUrl: 'app/components/about/licenses/licenses.html',
        controller: ['$http', licensesCtrl]
    });
    
    
    
    function licensesCtrl($http) {
        var ctrl = this;
        
        ctrl.$onInit = function init() {
        	$http.get('about/THIRD-PARTY.txt').success(function(res) {
                ctrl.thirdParty=res;
            });
        }
    }
    
})();
