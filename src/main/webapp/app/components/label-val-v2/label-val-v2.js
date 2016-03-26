(function () {

	'use strict';

	var components = angular.module('lavagna.components');

	components.component('lvgLabelValV2', {
		template: '<span ng-bind="::$ctrl.displayValue" ng-if="::($ctrl.type != \'CARD\' && $ctrl.type != \'USER\')"></span>' +
					'<a ng-href="{{::$ctrl.cardLink}}" ng-if="::($ctrl.type == \'CARD\')" class="lvg-label-val-v2-card" ng-bind="::$ctrl.displayValue"></a>' + 
					'<a href="" ng-if="::($ctrl.type == \'USER\')" class="lvg-label-val-v2-user"></a>',
		bindings: {
			valueRef: '&',
			projectMetadataRef:'&'
		},
		controller: function($filter, $element, $rootScope, $state, CardCache) {
			var ctrl = this;
			ctrl.value = ctrl.valueRef();
			
			var metadata = ctrl.projectMetadataRef();
			
			var type = ctrl.value.labelValueType || ctrl.value.type || ctrl.value.labelType;
			var value = ctrl.value.value || ctrl.value;
			
			ctrl.type = type;
			
			if (type === 'STRING') {
				ctrl.displayValue = value.valueString;
			} else if (type === 'INT') {
				ctrl.displayValue = value.valueInt;
			} else if (type === 'USER') {
				ctrl.displayValue = '__USER__' + value.valueUser;
				//FIXME
			} else if (type === 'CARD') {
				handleCard(value.valueCard);
			} else if (type === 'LIST' && metadata && metadata.labelListValues[value.valueList]) {
				ctrl.displayValue = metadata.labelListValues[value.valueList].value;
			} else if (type === 'TIMESTAMP') {
				ctrl.displayValue = $filter('date')(value.valueTimestamp, 'dd.MM.yyyy');
			}
			//
			
			
			function handleCard(cardId) {
				CardCache.card(cardId).then(function (card) {
					var element = $element.find("a");
					ctrl.displayValue = card.boardShortName + '-' + card.sequence;
					ctrl.cardLink = $state.href('board.card', {projectName: card.projectShortName, shortName: card.boardShortName, seqNr: card.sequence});
					
					updateCardClass(card, element);
					
					var toDismiss = $rootScope.$on('refreshCardCache-' + cardId, function () {
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
	});
})();