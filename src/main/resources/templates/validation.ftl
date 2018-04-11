<html>
<head>
    <title>Validation results</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
</head>
<body>
<div class="jumbotron text-center">
    <h2>Validation results</h2>
    <strong>${(body?exists && body.subscription?exists)?then(body.subscription.name, "Not found")}</strong>
</div>
<#if body?exists >
    <div class="container">

        <div class="row">
            <table class="table table-striped">
                <thead>
                <tr>
                    <th>#</th>
                    <th>Timestamp</th>
                    <th>Schema validation errors</th>
                    <th>Profile validation errors</th>
                    <th>XML</th>
                </tr>
                </thead>
                <tbody>
            <#list body.validationRefs as validation>
                <tr data-toggle="collapse" data-target="#accordion${validation?counter}" style="cursor: pointer" class="clickable ${validation.events?has_content?then("danger","success")}">
                    <th>${validation?counter}</th>
                    <td>${validation.schema.timestamp}</td>
                    <td>${validation.schema.events?size}</td>
                    <td>${validation.profile.events?size}</td>
                    <td><a href="validation/siri?validationRef=${validation.validationRef}">XML</a></td>
                </tr>
                <tr id="accordion${validation?counter}" class="collapse">
                    <td colspan="5">
                        <#if validation.schema.events?size != 0 >
                        <fieldset>
                            <legend>Schema validation</legend>
                            <table class="table table-striped">
                                <thead>
                                <tr>
                                    <th>Severity</th>
                                    <th>Count</th>
                                    <th>Message</th>
                                    <th>Line</th>
                                    <th>Column</th>
                                </tr>
                                </thead>
                                <tbody>
                                    <#list validation.schema.events?sort_by("numberOfOccurrences")?reverse as event >
                                    <tr>
                                        <td>${event.severity}</td>
                                        <td>${event.numberOfOccurrences}</td>
                                        <td>${event.message}</td>
                                        <td>${event.locator.lineNumber}</td>
                                        <td>${event.locator.columnNumber}</td>
                                    </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </fieldset>
                        </#if>
                        <#if validation.profile.events?size != 0 >
                        <fieldset>
                            <legend>Profile validation</legend>
                            <table class="table table-striped">
                                <thead>
                                <tr>
                                    <th>Count</th>
                                    <th>Message</th>
                                </tr>
                                </thead>
                                <tbody>
                                    <#list validation.profile.events?sort_by("numberOfOccurrences")?reverse as event >
                                    <tr>
                                        <td>${event.numberOfOccurrences}</td>
                                        <td>${event.message}</td>
                                    </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </fieldset>
                        </#if>
                    </td>
                </tr>
            </#list>
            </tbody>
            </table>
        </div>
    </div>
</#if>
</body>
</html>