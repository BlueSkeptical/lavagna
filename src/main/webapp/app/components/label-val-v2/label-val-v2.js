(function () {

	'use strict';

	var components = angular.module('lavagna.components');

	components.component('lvgLabelValV2', {
		bindings: {
			valueRef: '&',
			projectMetadataRef:'&'
		},
		controller: ['$filter', '$element', 'EventBus',
		    '$state', '$window', 'CardCache', 'Tooltip', 'UserCache', LabelValV2Controller]
	});

	function LabelValV2Controller($filter, $element, EventBus, $state, $window, CardCache, Tooltip, UserCache) {
		var ctrl = this;

		var elementDom = $element[0];

		var listeners = [];
        var mouseOverElements = [];

		ctrl.$postLink = function postLink() {

			var ctrl_value = ctrl.valueRef();
			var metadata = ctrl.projectMetadataRef();
			var type = ctrl_value.labelValueType || ctrl_value.type || ctrl_value.labelType;
			var value = ctrl_value.value || ctrl_value;

			if (type === 'STRING') {
				elementDom.textContent = value.valueString;
			} else if (type === 'INT') {
				elementDom.textContent = value.valueInt;
			} else if (type === 'USER') {
				handleUser(value.valueUser);
			} else if (type === 'CARD') {
				handleCard(value.valueCard, metadata);
			} else if (type === 'LIST' && metadata && metadata.labelListValues && metadata.labelListValues[value.valueList]) {
				elementDom.textContent = metadata.labelListValues[value.valueList].value;
			} else if (type === 'TIMESTAMP') {
				elementDom.textContent = $filter('date')(value.valueTimestamp, 'dd.MM.yyyy');
			}
		}

		ctrl.$onDestroy = function onDestroy() {
		    Tooltip.clean();

		    for(var i = 0; i < listeners.length; i++) {
                listeners[i]();
            }

            for(var i = 0; i < mouseOverElements.length; i++) {
                var element = mouseOverElements[i];
                element.removeEventListener('mouseenter', handleMouseEnter);
                element.removeEventListener('mouseleave', handleMouseLeave);
            }
		};

		//-------------
		function handleMouseEnter($event) {
            Tooltip.clean('lvg-tooltip-card-' + $event.target.card.id);
            Tooltip.card($event.target.card, function() {
                return $event.target.metadata
            }, null, $event.target);
        };

		function handleMouseLeave($event) {
		    Tooltip.close('lvg-tooltip-card-' + $event.target.card.id);
        };

		function handleUser(userId) {

			var a = $window.document.createElement('a');
			$element.append(a);

			UserCache.user(userId).then(function (user) {
				var element = angular.element(a);

				element.attr('href', $state.href('user.dashboard', {provider: user.provider, username: user.username}));

				element.text($filter('formatUser')(user));
				if (!user.enabled) {
					element.addClass('user-disabled');
				}

			});
		}
		//-------------

		function handleCard(cardId, metadata) {

			var a = $window.document.createElement('a');
			$element.append(a);

			CardCache.card(cardId).then(function (card) {
				var element = angular.element(a);

				a.textContent = card.boardShortName + '-' + card.sequence;
				a.card = card;
				a.metadata = metadata;
				element.attr('href', $state.href('board.card', {projectName: card.projectShortName, shortName: card.boardShortName, seqNr: card.sequence}));

				a.addEventListener('mouseenter', handleMouseEnter);
                a.addEventListener('mouseleave', handleMouseLeave);

				updateCardClass(card, element);

                mouseOverElements.push(a);

				var toDismiss = EventBus.on('refreshCardCache-' + cardId, function () {
					CardCache.card(cardId).then(function (card) {
						updateCardClass(card, element);
					});
				});

				listeners.push(toDismiss);
			});
		}
	}

	function updateCardClass(card, element) {
		if (card.columnDefinition !== 'CLOSED') {
			element.removeClass('lavagna-closed-card');
		} else {
			element.addClass('lavagna-closed-card');
		}
	}

})();
