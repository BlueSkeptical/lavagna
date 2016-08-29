(function() {


	var services = angular.module('lavagna.services');

	services.factory('BulkOperationModal', function ($mdDialog, Card, User, BulkOperations, Label, $translate) {

		function moveTo(toMove, location) {
			var title = $translate.instant('dialog-move-to.'+location);
			var confirm = $mdDialog.confirm()
				.title(title)
				.ariaLabel(title)
				.ok($translate.instant('button.yes'))
				.cancel($translate.instant('button.no'));

			$mdDialog.show(confirm).then(function() {
				for(var columnId in toMove) {
					Card.moveAllFromColumnToLocation(columnId, toMove[columnId], location);
				}
			}, function() {});
		}

		return {
			moveToArchive : function(toMove) {
				moveTo(toMove, 'ARCHIVE');
			},
			moveToBacklog : function(toMove) {
				moveTo(toMove, 'BACKLOG');
			},
			moveToTrash : function(toMove) {
				moveTo(toMove, 'TRASH');
			},

			assignTo: function(cards, applyIfPresent) {

				applyIfPresent = applyIfPresent || angular.noop;

				$mdDialog.show({
					template: '<lvg-dialog-select-user dialog-title="vm.title" cards="vm.cards" action="vm.action($user)"></lvg-dialog-select-user>',
					locals: {
                        title: 'dialog-select-user.assign',
                        action: function(user) {
                            BulkOperations.assign(cards, user).then(applyIfPresent);
                        }
                    },
                    bindToController: true,
                    controller: function() {},
                    controllerAs: 'vm'
				});
			},

			removeAssignTo: function(cards, applyIfPresent) {
				applyIfPresent = applyIfPresent || angular.noop;

				$mdDialog.show({
					template: '<lvg-dialog-select-user dialog-title="vm.title" cards="vm.cards" action="vm.action($user)"></lvg-dialog-select-user>',
					locals: {
					    title: 'dialog-select-user.remove',
					    action: function(user) {
					        BulkOperations.removeAssign(cards, user).then(applyIfPresent);
					    }
					},
					bindToController: true,
					controller: function() {},
					controllerAs: 'vm'
				});
			},

			reAssignTo: function(cards, applyIfPresent) {

				applyIfPresent = applyIfPresent || angular.noop;

				$mdDialog.show({
				    template: '<lvg-dialog-select-user dialog-title="vm.title" cards="vm.cards" action="vm.action($user)"></lvg-dialog-select-user>',
                    locals: {
                        title: 'dialog-select-user.reassign',
                        action: function(user) {
                            BulkOperations.reassign(cards, user).then(applyIfPresent);
                        }
                    },
                    bindToController: true,
                    controller: function() {},
                    controllerAs: 'vm'
				});
			},

			setDueDate : function(cards, applyIfPresent) {
				applyIfPresent = applyIfPresent || angular.noop;
				$mdDialog.show({
					template: '<lvg-dialog-select-date dialog-title="vm.title" action="vm.action($date)"></lvg-dialog-select-date>',
					locals: {
                        title: 'dialog-select-date.set',
                        action: function(dueDate) {
                            BulkOperations.setDueDate(cards, dueDate).then(applyIfPresent);
                        }
                    },
                    bindToController: true,
                    controller: function() {},
                    controllerAs: 'vm'
				});
			},

			removeDueDate: function removeDueDate(cards, applyIfPresent) {
				var title = $translate.instant('dialog-remove-due-date.title');
				var confirm = $mdDialog.confirm().title(title)
		          .ariaLabel(title)
		          .ok($translate.instant('button.yes'))
		          .cancel($translate.instant('button.no'));

				$mdDialog.show(confirm).then(function() {
					applyIfPresent = applyIfPresent || angular.noop;
					BulkOperations.removeDueDate(cards).then(applyIfPresent);
				}, function() {});
			},

			setMilestone: function(cards, projectName, applyIfPresent) {
				applyIfPresent = applyIfPresent || angular.noop;
				$mdDialog.show({
					template: '<lvg-dialog-select-milestone dialog-title="title" action="action($milestone)"  project-name="projectName"></lvg-dialog-select-milestone>',
					controller: function($scope) {
						$scope.title = 'SELECT MILESTONE';
						$scope.projectName = projectName;
						$scope.action = function(milestone) {
							BulkOperations.setMilestone(cards, milestone).then(applyIfPresent);
						}
					}
				});
			},

			removeMilestone: function(cards, applyIfPresent) {
				var title = $translate.instant('dialog-remove-milestone.title');
				var confirm = $mdDialog.confirm().title(title)
		          .ariaLabel(title)
		          .ok($translate.instant('button.yes'))
		          .cancel($translate.instant('button.no'));

				$mdDialog.show(confirm).then(function() {
					applyIfPresent = applyIfPresent || angular.noop;
					BulkOperations.removeMilestone(cards).then(applyIfPresent);
				}, function() {});
			},

			addLabel: function(cards, projectName, applyIfPresent) {
				applyIfPresent = applyIfPresent || angular.noop;
				$mdDialog.show({
					template: '<lvg-dialog-select-label dialog-title="title" action="action" project-name="projectName" with-label-value-picker="true"></lvg-dialog-select-label>',
					controller: function($scope) {
						$scope.title = 'FIXME SELECT LABEL TO ADD';
						$scope.projectName = projectName;
						$scope.action = function(labelToAdd, labelValueToAdd) {
							var labelValueToAdd = Label.extractValue(labelToAdd, labelValueToAdd);
							BulkOperations.addLabel(cards, labelToAdd, labelValueToAdd).then(applyIfPresent);
						}
					}
				});
			},

			removeLabel: function(cards, projectName, applyIfPresent) {
				applyIfPresent = applyIfPresent || angular.noop;
				$mdDialog.show({
					template: '<lvg-dialog-select-label dialog-title="title" action="action" project-name="projectName"></lvg-dialog-select-label>',
					controller: function($scope) {
						$scope.title = 'FIXME SELECT LABEL TO REMOVE';
						$scope.projectName = projectName;
						$scope.action = function(labelToRemove) {
							BulkOperations.removeLabel(cards, labelToRemove).then(applyIfPresent);
						}
					}
				});
			}
		};
	})


})();
