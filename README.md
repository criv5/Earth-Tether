	private Location getTargetLocation() {
		final Entity target = GeneralMethods.getTargetedEntity(this.player, this.range, new ArrayList<Entity>());
		Location location;
		final Material[] trans = new Material[getTransparentMaterials().length + this.getEarthbendableBlocks().size()];
		int i = 0;
		for (int j = 0; j < getTransparentMaterials().length; j++) {
			trans[j] = getTransparentMaterials()[j];
			i++;
		}
		for (int j = 0; j < this.getEarthbendableBlocks().size(); j++) {
			try {
				trans[i] = Material.valueOf(this.getEarthbendableBlocks().get(j));
			} catch (final IllegalArgumentException e) {
				continue;
			}
			i++;
		}

		if (target == null) {
			location = GeneralMethods.getTargetedLocation(this.player, this.range, true, trans);
		} else {
			location = ((LivingEntity) target).getEyeLocation();
		}

		return location;
	}
