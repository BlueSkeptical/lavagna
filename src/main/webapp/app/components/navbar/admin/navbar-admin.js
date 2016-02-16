(function() {
    var components = angular.module('lavagna.components');

    components.component('lvgNavbarAdmin', {
        templateUrl: 'app/components/navbar/admin/navbar-admin.html',
        bindings: {},
        controllerAs: 'lvgNavbarAdmin',
        controller: function($window, User, $mdSidenav, $rootScope, $state) {
             var ctrl = this;

             User.currentCachedUser().then(function (u) {
                 ctrl.navbarUser = u;
             });

             ctrl.toggleSidebar = function() {
            	 $mdSidenav('left').toggle();
             }

             ctrl.login = function () {
                 var reqUrlWithoutContextPath = $window.location.pathname.substr($("base").attr('href').length - 1);
                 $window.location.href = 'login?reqUrl=' + encodeURIComponent(reqUrlWithoutContextPath);
             }

             ctrl.navigationState = $state.current.name;
             $rootScope.$on('$stateChangeSuccess',
                function (event, toState, toParams, fromState, fromParams) {
                    ctrl.navigationState = $state.current.name;
                }
             );
        }
    });
})();
