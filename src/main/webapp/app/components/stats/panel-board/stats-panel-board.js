(function() {

	'use strict';

	angular.module('lavagna.components').component('lvgStatsPanelBoard', {
		templateUrl : 'app/components/stats/panel-board/stats-panel-board.html',
		bindings : {
			board: '<',
			projectShortName: '<'
		},
		controller : ['Board', statsPanelBoard]
	});
	
	
	function statsPanelBoard(Board) {

		var ctrl = this;

		ctrl.statsFetcher = statsFetcher;
		
		function statsFetcher() {
			return Board.taskStatistics(ctrl.board.shortName);
		}
	}
	
})();
