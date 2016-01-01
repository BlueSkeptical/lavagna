(function() {
    'use strict';

    angular.module('lavagna.components').component('lvgAboutLicenses', {
    	templateUrl: 'app/components/about/licenses/licenses.html',
        controller: function($http) {
            var ctrl = this;
            $http.get('about/THIRD-PARTY.txt').success(function(res) {
                ctrl.thirdParty=res;
            });
        }
    });
})();
