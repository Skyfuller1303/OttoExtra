package de.ottoextra.api;
import java.net.URI;
public record ApiProblem(Kind kind, String message, URI uri) {
    public enum Kind {
        OFFLINE,
        HTTP_STATUS,
        PARSE,
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
    public boolean isHttpStatus(int status) {
        return kind == Kind.HTTP_STATUS && ("HTTP " + status).equals(message);
    }
    public ApiException toException() {
        return new ApiException(this);
    }
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
