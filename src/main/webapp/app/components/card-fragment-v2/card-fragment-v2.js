(function () {
	'use strict';
	
	angular.module('lavagna.components').component('lvgCardFragmentV2', {
		template: '<div data-lvg-board-card-menu '
					+'data-ng-class="::{\'lavagna-is-watching\': !$ctrl.listView && $ctrl.isSelfWatching, \'lavagna-board-panel\' : $ctrl.boardView }">'
					+'<lvg-card-fragment-v2-head></lvg-card-fragment-v2-head>'
					+'<lvg-card-fragment-v2-data-info></lvg-card-fragment-v2-data-info>'
					+'</div>',
		bindings: {
			readOnly: '@', /* true | false (default) */
			view: '@', /* list | board | search */
			hideSelect: '@', /* "true" | ... */
			searchType: '@', /* globalSearch | projectSearch (default) */
			cardRef:'&',
			userRef:'&',
			projectMetadataRef: '&',
			selectedRef:'&'
		},
		controller: ['Card', CardFragmentV2Controller]
	});
	
	
	function CardFragmentV2Controller(Card) {
		const ctrl = this;
		
		ctrl.card = ctrl.cardRef();
		//
		ctrl.boardShortName = ctrl.card.boardShortName,
		ctrl.projectShortName = ctrl.card.projectShortName,
		//
		ctrl.user = ctrl.userRef();
		ctrl.projectMetadata = ctrl.projectMetadataRef();
		//
		ctrl.selected = ctrl.selectedRef();
		
		ctrl.readOnly = ctrl.readOnly != undefined;
        ctrl.listView = ctrl.view != undefined && ctrl.view == 'list';
        ctrl.boardView = ctrl.view != undefined && ctrl.view == 'board';
        ctrl.searchView = ctrl.view != undefined && ctrl.view == 'search';
        
        ctrl.shortCardName = ctrl.card.boardShortName + ' - ' + ctrl.card.sequence;
        
        if(ctrl.hideSelect === undefined) {
        	ctrl.hideSelect = false;
        }
        
        //
        ctrl.isSelfWatching = Card.isWatchedByUser(ctrl.card.labels, ctrl.user.id);
        ctrl.isAssignedToCard = Card.isAssignedToUser(ctrl.card.labels, ctrl.user.id);
        //
	}
	
})();