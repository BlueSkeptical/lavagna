(function () {

	'use strict';

	var directives = angular.module('lavagna.directives');

	directives.directive('lvgUserTooltip', function ($filter, $q, $rootScope, UserCache) {

		/*var providerMap = {
			'demo': 'fa-laptop',
			'ldap': 'fa-book',
			'persona': 'fa-user',
			'oauth.google': 'fa-google',
			'oauth.bitbucket': 'fa-bitbucket',
			'oauth.github': 'fa-github',
			'oauth.twitter': 'fa-twitter'
		};
		
		//TODO: this is a temporary fix
		function escapeHtml(unsafe) {
		    return unsafe
		         .replace(/&/g, "&amp;")
		         .replace(/</g, "&lt;")
		         .replace(/>/g, "&gt;")
		         .replace(/"/g, "&quot;")
		         .replace(/'/g, "&#039;");
		 }*/

		var generateTooltipHTML = function (user) {
			return '';
			/*var userDisplayText = $filter('formatUser')(user);
			var userProviderClass = providerMap[user.provider] || 'fa-laptop';
			return '<div class=\"lavagna-tooltip\">' +
				'<div class=\"provider\"><i class=\"fa ' + userProviderClass + '\"></i></div>' +
				'<div class=\"name\">' + escapeHtml(userDisplayText) + '</div>' +
				'<div class=\"user-info\"><ul>' +
				'<li><i class=\"fa fa-user\"></i>' + escapeHtml(user.username) + '</li>' +
				(user.email != null ? '<li><i class=\"fa fa-envelope\"></i><a href="mailto:' + escapeHtml(user.email) + '">' + escapeHtml(user.email) + '</a></li>' : '') +
				'</ul></div>' +
				'</div>';*/
		};

		var loadUser = function (userId, placeholder, providerPlaceholder, scope) {
			var deferred = $q.defer();
			UserCache.user(userId).then(function (user) {
				placeholder.text($filter('formatUser')(user));
				if (user.enabled) {
					placeholder.removeClass('user-disabled');
				} else {
					placeholder.addClass('user-disabled');
				}
				deferred.resolve(generateTooltipHTML(user));
				scope.user = user;
			});
			return deferred.promise;
		};

		return {
			restrict: 'A',
			scope: true,
			template: function($element, $attrs) {
				var readOnly = $attrs.readOnly != undefined;
				if(readOnly) {
					return '<span class="lvg-user-placeholder"></span></span>';
				} else {
					return '<a class="lvg-user-link-placeholder" ui-sref="user.dashboard({provider: user.provider, username: user.username})"><span class="lvg-user-placeholder"></span></span></a>'
				}
			},
			link: function ($scope, element, attrs) {

				var unregister = $scope.$watch(attrs.lvgUserTooltip, function (userId) {
					if (userId == undefined) {
						return;
					}
					var placeholder = element.find('.lvg-user-placeholder');
					var providerPlaceholder = element.find('.lvg-user-provider-placeholder');
					

					loadUser(userId, placeholder, providerPlaceholder, $scope).then(function (html) {
						//$scope.tooltipHTML = html;
					});

					var unbind = $rootScope.$on('refreshUserCache-' + userId, function () {
						loadUser(userId, placeholder, providerPlaceholder, linkPlaceholder).then(function (html) {
							//$scope.tooltipHTML = html;
						});
					});
					$scope.$on('$destroy', unbind);

					unregister();
				});
			}
		};
	});
})();
