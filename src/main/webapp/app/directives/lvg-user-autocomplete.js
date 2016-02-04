(function () {

    'use strict';

    var directives = angular.module('lavagna.directives');

    directives.directive('lvgUserAutocomplete', function ($http, $location, $parse, User) {
        return {
            restrict: 'A',
            link: function (scope, elem, attrs) {

                var formatName = function (user) {
                    if (user.displayName)
                        return user.displayName + ' (' + user.provider + ':' + user.username + ')';
                    return user.provider + ':' + user.username;
                };

                var useGlobal = attrs.lvgUserAutocompleteGlobal === 'true';

                scope.$watch(attrs['lvgUserAutocomplete'], function (newVal) {
                    $(elem).val(newVal ? formatName(newVal) : null);
                });

                $(elem).autocomplete({
                    minLength: 1,
                    focus: function (event) {
                        event.preventDefault();
                    },
                    source: function (request, response) {
                        (useGlobal ? User.findUsersGlobally : User.findUsers)(request.term.trim()).then(function (res) {
                            response($.map(res, function (user) {
                                return {
                                    label: formatName(user),
                                    value: user
                                };
                            }));
                        });
                    },
                    select: function (event, ui) {
                        event.preventDefault();
                        $(elem).val(ui.item.label);
                        scope.$apply(function () {
                            $parse(attrs['lvgUserAutocomplete']).assign(scope, ui.item.value);
                        });
                    }
                });
            }
        };
    });
})();
