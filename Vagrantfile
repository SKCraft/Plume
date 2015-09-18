Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"

  config.vm.network "forwarded_port", guest: 3000, host: 23000 # grafana
  config.vm.network "forwarded_port", guest: 2003, host: 22003 # line receiver
  config.vm.network "forwarded_port", guest: 2004, host: 22004 # pickle receiver
  config.vm.network "forwarded_port", guest: 7002, host: 27002 # query
  config.vm.network "forwarded_port", guest: 80, host: 20080 # web server

  config.vm.provider "virtualbox" do |vb|
    vb.gui = false
    vb.memory = "512"
  end

  # spend less time waiting
  if Vagrant.has_plugin?("vagrant-cachier")
    config.cache.scope = :box
  end

  config.vm.provision "shell", inline: <<-SHELL
    apt-get update
    DEBIAN_FRONTEND=noninteractive apt-get install -y graphite-web graphite-carbon apache2 libapache2-mod-wsgi adduser libfontconfig
    cp /vagrant/vagrantdata/graphite-carbon /etc/default/graphite-carbon
    cp /vagrant/vagrantdata/carbon.conf /etc/carbon/carbon.conf
    graphite-manage syncdb --noinput
    service carbon-cache start
    a2dissite 000-default
    cp /usr/share/graphite-web/apache2-graphite.conf /etc/apache2/sites-available
    a2ensite apache2-graphite
    service apache2 reload
    chmod o+rw /var/lib/graphite/graphite.db
    wget -nv https://grafanarel.s3.amazonaws.com/builds/grafana_2.1.3_amd64.deb
    dpkg -i grafana_2.1.3_amd64.deb
    sudo update-rc.d grafana-server defaults 95 10
    sudo service grafana-server start
  SHELL
end
