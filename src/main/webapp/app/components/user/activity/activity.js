(function() {
    'use strict';

    var components = angular.module('lavagna.components');

    components.component('lvgUserActivity', {
        bindings: {
            profile: '<'
        },
        controller: UserActivityController,
        templateUrl: 'app/components/user/activity/activity.html'
    });

    function UserActivityController(User) {
        var ctrl = this;


        ctrl.userProvider = ctrl.profile.user.provider;
        ctrl.userName = ctrl.profile.user.username;

        ctrl.page = 0;

        ctrl.loadFor = function (page) {
            User.getUserProfile(ctrl.userProvider, ctrl.userName, page)
                .then(function (profile) {
                    return loadUser(profile);
                })
                .then(function () {
                    ctrl.page = page
                });
        };

        //init
        loadUser(ctrl.profile);
        
        
        function loadUser (profile) {
            ctrl.profile = profile;
            ctrl.user = profile.user;

            ctrl.hasMore = profile.latestActivityByPage.length > 20;
            ctrl.activeProjects = profile.activeProjects;

            ctrl.latestActivity20 = profile.latestActivityByPage.slice(0, 20);
        };
    }

})();
