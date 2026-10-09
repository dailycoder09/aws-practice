# Local stand-in for the ALB: nginx applying the same path rules (see gateway.conf).
FROM docker.io/nginxinc/nginx-unprivileged:1.27-alpine
COPY gateway.conf /etc/nginx/conf.d/default.conf
# The file keeps the mode it has on the build machine (a 640 file is unreadable for the non-root nginx user),
# so set it explicitly. This works with every builder; COPY --chmod needs BuildKit.
USER root
RUN chmod 644 /etc/nginx/conf.d/default.conf
USER 101
EXPOSE 8080
