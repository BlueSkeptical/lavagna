(function() {
	
	var module = angular.module('lavagna-setup');
	
	module.component('setupThirdStep', {
		controller: SetupUserCtrl,
		templateUrl: 'components/third-step/third-step.html'
	});
	
	
	function SetupUserCtrl($window, Configuration, $http, $state) {
		
		var ctrl = this;

		ctrl.authMethod = Configuration.selectedAuthMethod;
		ctrl.loginType = Configuration.loginType;
		

		if (Configuration.toSave.user && Configuration.loginType.indexOf(Configuration.toSave.user.provider) > -1) {
			ctrl.accountProvider = Configuration.toSave.user.provider;
			ctrl.username = Configuration.toSave.user.username;
		}

		ctrl.saveUser = function () {
			Configuration.toSave.user = {
				provider: ctrl.accountProvider,
				username: ctrl.username,
				enabled: true,
				roles: ['ADMIN']
			};
			$state.go('fourth-step');
		};
		
		ctrl.back = function() {
			$state.go('second-step');
		}
	}
	
})();