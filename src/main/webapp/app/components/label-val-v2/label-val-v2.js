(function () {

	'use strict';

	var components = angular.module('lavagna.components');

	components.component('lvgLabelValV2', {
		bindings: {
			valueRef: '&',
			projectMetadataRef:'&'
		},
		controller: ['$filter', '$element', '$rootScope', '$state', '$window', 'CardCache', 'UserCache', lvgLabelValV2Ctrl]
	});
	
	function lvgLabelValV2Ctrl($filter, $element, $rootScope, $state, $window, CardCache, UserCache) {
		const ctrl = this;
		const ctrl_value = ctrl.valueRef();
		
		const metadata = ctrl.projectMetadataRef();
		
		const type = ctrl_value.labelValueType || ctrl_value.type || ctrl_value.labelType;
		const value = ctrl_value.value || ctrl_value;
		
		ctrl.$postLink = function lvgLabelValV2PostLink() {
			if (type === 'STRING') {
				appendValueToSpan(value.valueString);
			} else if (type === 'INT') {
				appendValueToSpan(value.valueInt);
			} else if (type === 'USER') {
				handleUser(value.valueUser);
			} else if (type === 'CARD') {
				handleCard(value.valueCard);
			} else if (type === 'LIST' && metadata && metadata.labelListValues && metadata.labelListValues[value.valueList]) {
				appendValueToSpan(metadata.labelListValues[value.valueList].value);
			} else if (type === 'TIMESTAMP') {
				appendValueToSpan($filter('date')(value.valueTimestamp, 'dd.MM.yyyy'));
			}
		}
		
		//-------------
		
		function appendValueToSpan(value) {
			const span = $window.document.createElement('span');
			span.textContent = value;
			$element.append(span);
		}
		
		function handleUser(userId) {
			
			const a = $window.document.createElement('a');
			$element.append(a);
			
			UserCache.user(userId).then(function (user) {
				const element = angular.element(a);
				
				element.attr('href', $state.href('user.dashboard', {provider: user.provider, username: user.username}));
				
				updateUser(user, element);
				
				const toDismiss = $rootScope.$on('refreshUserCache-' + userId, function () {
					UserCache.user(userId).then(function(user) {
						updateUser(user, element);
					})
				});
				
				ctrl.$onDestroy = function onDestroy() {
					toDismiss();
				};
			});
		}
		
		function updateUser(user, element) {
			//CHECK
			element.text($filter('formatUser')(user));
			if (user.enabled) {
				element.removeClass('user-disabled');
			} else {
				element.addClass('user-disabled');
			}
		}
		//-------------
		
		function handleCard(cardId) {
			
			const a = $window.document.createElement('a');
			$element.append(a);
			
			CardCache.card(cardId).then(function (card) {
				const element = angular.element(a);
				
				a.textContent = card.boardShortName + '-' + card.sequence;
				element.attr('href', $state.href('board.card', {projectName: card.projectShortName, shortName: card.boardShortName, seqNr: card.sequence}));
				
				updateCardClass(card, element);
				
				const toDismiss = $rootScope.$on('refreshCardCache-' + cardId, function () {
					CardCache.card(cardId).then(function (card) {
						updateCardClass(card, element);
					});
				});
				
				ctrl.$onDestroy = function onDestroy() {
					toDismiss();
				};
			});
		}
		
		function updateCardClass(card, element) {
			if (card.columnDefinition != 'CLOSED') {
				element.removeClass('lavagna-closed-card');
			} else {
				element.addClass('lavagna-closed-card');
			}
		}
	}
	
	
})();