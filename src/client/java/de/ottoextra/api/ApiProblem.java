package de.ottoextra.api;

import java.net.URI;

/**
 * Einheitliche, spielerfreundliche Fehlerrepräsentation der API-Schicht.
 *
 * <p>Module bekommen ein {@code ApiProblem} statt roher Exceptions/Stacktraces.
 * Niemals einen Stacktrace im Spiel anzeigen.</p>
 */
public record ApiProblem(Kind kind, String message, URI uri) {

    public enum Kind {
        /** Netzwerk nicht erreichbar / Timeout. */
        OFFLINE,
        /** HTTP-Statuscode != 2xx. */
        HTTP_STATUS,
        /** Antwort konnte nicht geparst werden. */
        PARSE,
        /** Ungültige Anfrage (z. B. fehlende UUID). */
        BAD_REQUEST
    }

    public static ApiProblem offline(URI uri, String detail) {
        return new ApiProblem(Kind.OFFLINE, detail, uri);
    }

    public static ApiProblem httpStatus(URI uri, int status) {
        return new ApiProblem(Kind.HTTP_STATUS, "HTTP " + status, uri);
    }

    public static ApiProblem parse(URI uri, String detail) {
        return new ApiProblem(Kind.PARSE, detail, uri);
    }

    public static ApiProblem badRequest(String detail) {
        return new ApiProblem(Kind.BAD_REQUEST, detail, null);
    }

    /** Ist dieses Problem genau der gegebene HTTP-Status? */
    public boolean isHttpStatus(int status) {
        return kind == Kind.HTTP_STATUS && ("HTTP " + status).equals(message);
    }

    /** Als Exception für CompletableFuture-Verkettung. */
    public ApiException toException() {
        return new ApiException(this);
    }

    /** Trägt ein {@link ApiProblem} durch CompletableFuture-Fehlerpfade. */
    public static final class ApiException extends RuntimeException {
        private final transient ApiProblem problem;

        public ApiException(ApiProblem problem) {
            super(problem.kind() + ": " + problem.message());
            this.problem = problem;
        }

        public ApiProblem problem() {
            return problem;
        }
    }
}
