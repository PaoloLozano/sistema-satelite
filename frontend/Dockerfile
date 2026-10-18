FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/
COPY assets /usr/share/nginx/html/assets
COPY pages /usr/share/nginx/html/pages
COPY partials /usr/share/nginx/html/partials
EXPOSE 80
