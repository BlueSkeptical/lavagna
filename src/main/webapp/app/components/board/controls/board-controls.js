(function() {
    'use strict';

    var components = angular.module('lavagna.components');

    components.component('lvgBoardControls', {
        bindings: {
            toggledSidebar: '=',
            sideBarLocation:'=',
            unSelectAll: '=',
            selectAll: '=',
            selectedVisibleCount: '=',
            formatBulkRequest: '=',
            selectedVisibleCardsIdByColumnId: '='
        },
        templateUrl: 'app/components/board/controls/board-controls.html',
        controller:function() {
        	var ctrl = this;
        	
        	ctrl.sideBarLocation = 'BOARD';
        	ctrl.selectForSidebar = function() {
        		ctrl.toggledSidebar = ctrl.sideBarLocation !== 'BOARD';
        	};
        }
    });
})();
