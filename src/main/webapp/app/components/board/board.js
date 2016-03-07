(function () {
    'use strict';

    var components = angular.module('lavagna.components');

    components.component('lvgBoard', {
        controller: BoardController,
        controllerAs: 'boardCtrl',
        bindings: {
            project: '=',
            board: '='
        },
        templateUrl: 'app/components/board/board.html'
    });

    function BoardController($rootScope, $scope, $location, $filter, $log, $timeout,
        Board, Card, Project, LabelCache, Search, StompClient, User, Notification) {

        var ctrl = this;
        ctrl.searchFilter = {};

        var boardName = ctrl.board.shortName;
        var projectName = ctrl.project.shortName;


        User.currentCachedUser().then(function(currentUser) {
            ctrl.currentUserId = currentUser.id;
        });

        ctrl.moveCard = function(card, location) {
            Card.moveAllFromColumnToLocation(card.columnId, [card.id], location);
        };

        ctrl.backFromLocation = function() {
            ctrl.locationOpened=false;
            ctrl.sideBarLocation=undefined;
        };

        ctrl.labelNameToId = {};
        var loadLabel = function() {
            LabelCache.findByProjectShortName(projectName).then(function(labels) {
                ctrl.labelNameToId = {};
                for(var k in labels) {
                    ctrl.labelNameToId[labels[k].name] = k;
                }
            });
        };
        loadLabel();


        var unbind = $rootScope.$on('refreshLabelCache-' + projectName, function() {
            loadLabel();
            $scope.$broadcast('loadcards');
        });
        $scope.$on('$destroy', unbind);

        //keep track of the selected cards
        ctrl.selectedCards = {};
        //ctrl.foundCards = {};

        ctrl.editMode = false;
        ctrl.switchEditMode = function() {
            ctrl.editMode = !ctrl.editMode;
        };


        ctrl.selectAll = function() {
            $scope.$broadcast('selectall');
        };

        ctrl.unSelectAll = function() {
            $scope.$broadcast('unselectall');
        };

        var selectedVisibleCardsId = function() {
            var ids = [];

            angular.forEach(selectedVisibleCardsIdByColumnId(), function(val) {
            	ids = ids.concat(val);
            });

            return ids;
        };

        var selectedVisibleCardsIdByColumnId = function() {
            var res = {};
            angular.forEach(ctrl.selectedCards, function(column, columnId) {
            	angular.forEach(column, function(isSelected, cardId) {
            		if(isSelected) {
	            		if(!res[columnId]) {
	            			res[columnId] = [];
	            		}
	            		res[columnId].push(parseInt(cardId,10));
            		}

            	})
            })
            return res;
        };

        ctrl.selectedVisibleCount = function() {
        	return selectedVisibleCardsId().length;
        };


        $scope.$on('refreshSearch', function(ev, searchFilter) {
            User.currentCachedUser().then(function(user) {
                try {
                    Search.buildSearchFilter(searchFilter.searchFilter, ctrl.columns, user.id).then(function(filterFun) {
                        ctrl.searchFilter.cardFilter = filterFun;
                        $timeout(function() {
                            $location.search(searchFilter.location);
                        });
                    });

                } catch(e) {
                    $log.debug('parsing exception', e);
                }
            });
        });
        //

        //-----------------------------------------------------------------------------------------------------
        //--- TODO cleanup

        //

        Project.columnsDefinition(projectName).then(function(definitions) {
            ctrl.columnsDefinition = definitions;
        });
        //

        ctrl.hasMetadata = function(card) {
            if(card.counts == null)
                return false; //empty
            return card.counts['COMMENT'] != undefined || card.counts['FILE'] != undefined
                || card.counts['ACTION_CHECKED'] != undefined || card.counts['ACTION_UNCHECKED'] != undefined;
        };


        ctrl.isEmpty = function(obj) {
            return Object.keys(obj).length === 0;
        };

        //----------
        ctrl.columnsLocation = 'BOARD';

        var assignToColumn = function(columns) {
            ctrl.columns = columns;
            $rootScope.$broadcast('requestSearch');
        };

        StompClient.subscribe($scope, '/event/board/'+boardName+'/location/BOARD/column', function() {
            Board.columns(boardName, ctrl.columnsLocation).then(assignToColumn);
        });

        Board.columns(boardName, 'BOARD').then(assignToColumn);

        //-------------

        ctrl.sortColumns = function() {
        	var colPos = [];
        	angular.forEach(ctrl.columns, function(col) {
        		colPos.push(col.id);
        	});
        	Board.reorderColumn(boardName, ctrl.columnsLocation, colPos).catch(function(error) {
                Notification.addAutoAckNotification('error', { key : 'notification.generic.error'}, false);
            });
        };
        
        ctrl.sortCards = function($item, $partFrom, $partTo, $indexFrom, $indexTo) {
        	
        	var oldColumnId = $partFrom.columnId;
        	var newColumnId = $partTo.columnId;
        	var cardId = $item.id;
        	var ids = [];
        	angular.forEach($partTo, function(card) {
        		ids.push(card.id);
        	});

        	if(oldColumnId === newColumnId) {
                Board.updateCardOrder(boardName, oldColumnId, ids).catch(function(error) {
                    Notification.addAutoAckNotification('error', { key : 'notification.generic.error'}, false);
                });
            } else {
                Board.moveCardToColumn(cardId, oldColumnId, newColumnId, {newContainer: ids}).catch(function(error) {
                    Notification.addAutoAckNotification('error', { key : 'notification.generic.error'}, false);
                });
            }
        }

        //will be used as a map columnState[columnId].editColumnName = true/false
        ctrl.columnState = {};

        ctrl.createColumn = function(columnToCreate) {
            Board.createColumn(boardName, columnToCreate).then(function() {
                columnToCreate.name = null;
                columnToCreate.definition = null;
            }).catch(function(error) {
                Notification.addAutoAckNotification('error', { key : 'notification.board.create-column.error'}, false);
            });
        };

        //-----------------------------------------------------------------------------------------------------

        var formatBulkRequest = function() {
            var r = {};
            r[projectName] = selectedVisibleCardsId();
            return r;
        };

        ctrl.formatBulkRequest = formatBulkRequest;
        ctrl.selectedVisibleCardsIdByColumnId = selectedVisibleCardsIdByColumnId;

        //-----------------------------------------------------------------------------------------------------

        //some sidebar controls
        ctrl.toggledSidebar = false;

        ctrl.toggleSidebar = function() {
            ctrl.toggledSidebar = !ctrl.toggledSidebar;
        };
    }
})();
