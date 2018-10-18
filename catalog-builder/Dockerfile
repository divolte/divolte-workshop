# Divolte Shop Webapp Demo image
#
# Divolte Documentation:
# www.divolte.io
#

# Pinned to 3.6, to avoid extra image downloads.
FROM python:3.6

# Create a system account so we don't have to run with root privileges.
RUN useradd -d /scripts -r loader

# Set up the python environment.
COPY requirements.txt /tmp/
RUN pip install --no-cache-dir -r /tmp/requirements.txt && \
    rm /tmp/requirements.txt

# Copy in the scripts
COPY *.py /scripts/

USER loader
ENTRYPOINT [ "/scripts/put-categories.py" ]
